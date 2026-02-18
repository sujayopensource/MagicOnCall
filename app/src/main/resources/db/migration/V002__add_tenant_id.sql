-- ============================================================
-- V002: Add multi-tenancy support (X-Customer-Id)
-- ============================================================

ALTER TABLE alerts ADD COLUMN tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
CREATE INDEX idx_alerts_tenant_id ON alerts (tenant_id);

ALTER TABLE outbox_events ADD COLUMN tenant_id VARCHAR(100) NOT NULL DEFAULT 'default';
CREATE INDEX idx_outbox_tenant_id ON outbox_events (tenant_id);
