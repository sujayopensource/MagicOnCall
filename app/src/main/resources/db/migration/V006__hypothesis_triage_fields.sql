-- ============================================================
-- V006: Hypothesis Engine — AI triage fields on hypotheses
-- ============================================================

ALTER TABLE hypotheses ADD COLUMN evidence_for    TEXT;
ALTER TABLE hypotheses ADD COLUMN evidence_against TEXT;
ALTER TABLE hypotheses ADD COLUMN next_best_test   TEXT;
ALTER TABLE hypotheses ADD COLUMN stop_condition   TEXT;
ALTER TABLE hypotheses ADD COLUMN evidence_hash    VARCHAR(64);

CREATE INDEX idx_hypotheses_evidence_hash ON hypotheses (evidence_hash);
