-- Correlation graph: nodes and edges for incident root cause analysis

CREATE TABLE correlation_nodes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    incident_id     UUID NOT NULL REFERENCES incidents(id),
    node_type       VARCHAR(50) NOT NULL CHECK (node_type IN ('ALERT','METRIC_ANOMALY','LOG_CLUSTER','DEPLOY','SERVICE','DEPENDENCY')),
    label           VARCHAR(255) NOT NULL,
    description     TEXT,
    reference_id    UUID,
    source          VARCHAR(255),
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_correlation_nodes_incident_id ON correlation_nodes(incident_id);
CREATE INDEX idx_correlation_nodes_node_type ON correlation_nodes(incident_id, node_type);
CREATE INDEX idx_correlation_nodes_reference_id ON correlation_nodes(incident_id, reference_id);

CREATE TABLE correlation_edges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    incident_id     UUID NOT NULL REFERENCES incidents(id),
    source_node_id  UUID NOT NULL REFERENCES correlation_nodes(id),
    target_node_id  UUID NOT NULL REFERENCES correlation_nodes(id),
    edge_type       VARCHAR(50) NOT NULL CHECK (edge_type IN ('TIME_CORRELATION','DEPENDS_ON','CAUSAL_HINT','SAME_RELEASE')),
    weight          DOUBLE PRECISION NOT NULL CHECK (weight >= 0.0 AND weight <= 1.0),
    reason          TEXT,
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    version         BIGINT NOT NULL DEFAULT 0,
    CHECK (source_node_id != target_node_id)
);

CREATE INDEX idx_correlation_edges_incident_id ON correlation_edges(incident_id);
CREATE INDEX idx_correlation_edges_source_node ON correlation_edges(source_node_id);
CREATE INDEX idx_correlation_edges_target_node ON correlation_edges(target_node_id);
