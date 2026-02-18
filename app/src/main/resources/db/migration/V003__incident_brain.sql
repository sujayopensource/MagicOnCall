-- ============================================================
-- V003: Incident Brain — core incident management tables
-- Tables: incidents, incident_events, hypotheses, evidence,
--         actions, audit_log
-- ============================================================

-- Incidents (aggregate root)
CREATE TABLE incidents (
    id               UUID PRIMARY KEY,
    tenant_id        VARCHAR(100) NOT NULL,
    external_id      VARCHAR(255) NOT NULL UNIQUE,
    title            VARCHAR(500) NOT NULL,
    summary          TEXT,
    severity         VARCHAR(20)  NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'NEW',
    commander_name   VARCHAR(255),
    slack_channel_id VARCHAR(255),
    tags             JSONB        DEFAULT '{}',
    created_by       VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version          BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_incident_severity CHECK (severity IN ('SEV1', 'SEV2', 'SEV3', 'SEV4')),
    CONSTRAINT chk_incident_status   CHECK (status IN ('NEW', 'TRIAGING', 'INVESTIGATING', 'MITIGATING', 'MONITORING', 'RESOLVED', 'POSTMORTEM'))
);

CREATE INDEX idx_incidents_tenant_id   ON incidents (tenant_id);
CREATE INDEX idx_incidents_external_id ON incidents (external_id);
CREATE INDEX idx_incidents_status      ON incidents (status);
CREATE INDEX idx_incidents_severity    ON incidents (severity);
CREATE INDEX idx_incidents_created_at  ON incidents (created_at DESC);

-- Incident Events (append-only timeline)
CREATE TABLE incident_events (
    id           UUID PRIMARY KEY,
    tenant_id    VARCHAR(100) NOT NULL,
    incident_id  UUID         NOT NULL REFERENCES incidents(id),
    event_type   VARCHAR(50)  NOT NULL,
    description  TEXT         NOT NULL,
    metadata     JSONB        DEFAULT '{}',
    created_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version      BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_incident_events_incident ON incident_events (incident_id, created_at ASC);

-- Hypotheses
CREATE TABLE hypotheses (
    id           UUID PRIMARY KEY,
    tenant_id    VARCHAR(100)     NOT NULL,
    incident_id  UUID             NOT NULL REFERENCES incidents(id),
    title        VARCHAR(500)     NOT NULL,
    description  TEXT,
    status       VARCHAR(30)      NOT NULL DEFAULT 'PROPOSED',
    confidence   DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    source       VARCHAR(20)      NOT NULL DEFAULT 'HUMAN',
    created_by   VARCHAR(255)     NOT NULL DEFAULT 'system',
    created_at   TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
    version      BIGINT           NOT NULL DEFAULT 0,

    CONSTRAINT chk_hypothesis_status CHECK (status IN ('PROPOSED', 'INVESTIGATING', 'CONFIRMED', 'REJECTED')),
    CONSTRAINT chk_hypothesis_source CHECK (source IN ('HUMAN', 'AI'))
);

CREATE INDEX idx_hypotheses_incident ON hypotheses (incident_id, created_at DESC);

-- Evidence
CREATE TABLE evidence (
    id            UUID PRIMARY KEY,
    tenant_id     VARCHAR(100) NOT NULL,
    incident_id   UUID         NOT NULL REFERENCES incidents(id),
    hypothesis_id UUID         REFERENCES hypotheses(id),
    evidence_type VARCHAR(30)  NOT NULL,
    title         VARCHAR(500) NOT NULL,
    content       TEXT,
    source_url    VARCHAR(2000),
    metadata      JSONB        DEFAULT '{}',
    created_by    VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version       BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_evidence_type CHECK (evidence_type IN ('LOG', 'METRIC', 'TRACE', 'ALERT', 'RUNBOOK', 'SCREENSHOT', 'OTHER'))
);

CREATE INDEX idx_evidence_incident   ON evidence (incident_id, created_at DESC);
CREATE INDEX idx_evidence_hypothesis ON evidence (hypothesis_id);

-- Actions
CREATE TABLE actions (
    id          UUID PRIMARY KEY,
    tenant_id   VARCHAR(100) NOT NULL,
    incident_id UUID         NOT NULL REFERENCES incidents(id),
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    status      VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    assignee    VARCHAR(255),
    action_type VARCHAR(30)  NOT NULL DEFAULT 'MANUAL',
    created_by  VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version     BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_action_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'SKIPPED')),
    CONSTRAINT chk_action_type   CHECK (action_type IN ('MANUAL', 'AUTOMATED', 'AI_SUGGESTED'))
);

CREATE INDEX idx_actions_incident ON actions (incident_id);

-- Audit Log
CREATE TABLE audit_log (
    id             UUID PRIMARY KEY,
    tenant_id      VARCHAR(100) NOT NULL,
    incident_id    UUID         NOT NULL REFERENCES incidents(id),
    entity_type    VARCHAR(50)  NOT NULL,
    entity_id      UUID         NOT NULL,
    action         VARCHAR(30)  NOT NULL,
    previous_state JSONB,
    new_state      JSONB,
    created_by     VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version        BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_audit_log_incident ON audit_log (incident_id, created_at ASC);
CREATE INDEX idx_audit_log_entity   ON audit_log (entity_type, entity_id);
