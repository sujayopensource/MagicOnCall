-- ============================================================
-- V005: Tool Framework — tool_execution_log table
--       + add TOOL_OUTPUT to evidence_type CHECK constraint
-- ============================================================

CREATE TABLE tool_execution_log (
    id               UUID PRIMARY KEY,
    tenant_id        VARCHAR(100)  NOT NULL,
    tool_name        VARCHAR(100)  NOT NULL,
    incident_id      UUID          REFERENCES incidents(id),
    status           VARCHAR(30)   NOT NULL,
    request_payload  JSONB         DEFAULT '{}',
    response_content TEXT,
    error_message    TEXT,
    duration_ms      BIGINT        NOT NULL DEFAULT 0,
    retry_count      INTEGER       NOT NULL DEFAULT 0,
    requested_by     VARCHAR(255)  NOT NULL DEFAULT 'system',
    executed_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(255)  NOT NULL DEFAULT 'system',
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT chk_tool_exec_status CHECK (status IN ('SUCCESS', 'FAILURE', 'TIMEOUT', 'RATE_LIMITED'))
);

CREATE INDEX idx_tool_exec_log_tenant     ON tool_execution_log (tenant_id);
CREATE INDEX idx_tool_exec_log_tool       ON tool_execution_log (tool_name, created_at DESC);
CREATE INDEX idx_tool_exec_log_incident   ON tool_execution_log (incident_id, created_at DESC);
CREATE INDEX idx_tool_exec_log_created_at ON tool_execution_log (created_at DESC);

-- Update evidence_type CHECK to include TOOL_OUTPUT
ALTER TABLE evidence DROP CONSTRAINT chk_evidence_type;
ALTER TABLE evidence ADD CONSTRAINT chk_evidence_type
    CHECK (evidence_type IN ('LOG', 'METRIC', 'TRACE', 'ALERT', 'RUNBOOK', 'SCREENSHOT', 'OTHER', 'TOOL_OUTPUT'));
