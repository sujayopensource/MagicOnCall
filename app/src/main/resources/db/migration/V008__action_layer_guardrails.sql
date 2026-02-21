-- ============================================================
-- V008: Action Layer with Guardrails
-- Extends actions table with guardrail lifecycle columns
-- ============================================================

-- Add guardrail columns to actions table
ALTER TABLE actions ADD COLUMN risk_level VARCHAR(30);
ALTER TABLE actions ADD COLUMN tool_name VARCHAR(255);
ALTER TABLE actions ADD COLUMN tool_parameters JSONB DEFAULT '{}';
ALTER TABLE actions ADD COLUMN idempotency_key VARCHAR(255);
ALTER TABLE actions ADD COLUMN requires_approval BOOLEAN DEFAULT FALSE;
ALTER TABLE actions ADD COLUMN approved_by VARCHAR(255);
ALTER TABLE actions ADD COLUMN approved_at TIMESTAMPTZ;
ALTER TABLE actions ADD COLUMN rejected_by VARCHAR(255);
ALTER TABLE actions ADD COLUMN rejected_at TIMESTAMPTZ;
ALTER TABLE actions ADD COLUMN rejection_reason TEXT;
ALTER TABLE actions ADD COLUMN policy_decision_reason TEXT;
ALTER TABLE actions ADD COLUMN policy_applied_rules VARCHAR(1000);
ALTER TABLE actions ADD COLUMN retry_count INTEGER DEFAULT 0;
ALTER TABLE actions ADD COLUMN max_retries INTEGER DEFAULT 3;
ALTER TABLE actions ADD COLUMN tool_execution_id UUID;
ALTER TABLE actions ADD COLUMN last_error TEXT;
ALTER TABLE actions ADD COLUMN proposed_by VARCHAR(255);

-- Drop and recreate CHECK constraints with new enum values
ALTER TABLE actions DROP CONSTRAINT chk_action_status;
ALTER TABLE actions ADD CONSTRAINT chk_action_status
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'SKIPPED',
                      'PROPOSED', 'APPROVED', 'EXECUTING', 'REJECTED'));

ALTER TABLE actions DROP CONSTRAINT chk_action_type;
ALTER TABLE actions ADD CONSTRAINT chk_action_type
    CHECK (action_type IN ('MANUAL', 'AUTOMATED', 'AI_SUGGESTED', 'GUARDRAILED'));

-- Risk level constraint
ALTER TABLE actions ADD CONSTRAINT chk_action_risk_level
    CHECK (risk_level IS NULL OR risk_level IN ('READ', 'SAFE_WRITE', 'DANGEROUS'));

-- Unique partial index on idempotency_key (only non-null values)
CREATE UNIQUE INDEX idx_actions_idempotency_key ON actions (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Additional indexes
CREATE INDEX idx_actions_status ON actions (status);
CREATE INDEX idx_actions_risk_incident ON actions (risk_level, incident_id);
CREATE INDEX idx_actions_tool_name ON actions (tool_name);
