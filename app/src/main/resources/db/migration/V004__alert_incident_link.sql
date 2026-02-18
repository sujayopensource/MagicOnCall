-- ============================================================
-- V004: Link alerts to incidents
-- ============================================================

ALTER TABLE alerts ADD COLUMN incident_id UUID REFERENCES incidents(id);

CREATE INDEX idx_alerts_incident_id ON alerts (incident_id);
