-- ============================================================
-- V001: Initial schema for MagicOnCall
-- Tables: alerts, outbox_events
-- ============================================================

-- Alerts table
CREATE TABLE alerts (
    id              UUID PRIMARY KEY,
    external_id     VARCHAR(255) NOT NULL UNIQUE,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    source          VARCHAR(255) NOT NULL,
    severity        VARCHAR(50)  NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    labels          JSONB        DEFAULT '{}',
    policy_decision_reason VARCHAR(1000) NOT NULL,
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT chk_alert_severity CHECK (severity IN ('INFO', 'WARNING', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_alert_status   CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'SUPPRESSED'))
);

CREATE INDEX idx_alerts_external_id ON alerts (external_id);
CREATE INDEX idx_alerts_status      ON alerts (status);
CREATE INDEX idx_alerts_severity    ON alerts (severity);
CREATE INDEX idx_alerts_source      ON alerts (source);
CREATE INDEX idx_alerts_created_at  ON alerts (created_at DESC);

-- Outbox events table (transactional outbox pattern)
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY,
    event_type      VARCHAR(255) NOT NULL,
    aggregate_type  VARCHAR(255) NOT NULL,
    aggregate_id    UUID         NOT NULL,
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,

    CONSTRAINT chk_published_at CHECK (
        (published = FALSE AND published_at IS NULL) OR
        (published = TRUE AND published_at IS NOT NULL)
    )
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at ASC) WHERE published = FALSE;
CREATE INDEX idx_outbox_aggregate   ON outbox_events (aggregate_type, aggregate_id);
