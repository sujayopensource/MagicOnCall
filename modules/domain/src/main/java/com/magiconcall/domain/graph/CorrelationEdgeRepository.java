package com.magiconcall.domain.graph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorrelationEdgeRepository {

    CorrelationEdge save(CorrelationEdge edge);

    Optional<CorrelationEdge> findById(UUID id);

    List<CorrelationEdge> findByIncidentId(UUID incidentId);

    List<CorrelationEdge> findBySourceNodeId(UUID sourceNodeId);

    List<CorrelationEdge> findByTargetNodeId(UUID targetNodeId);

    void deleteById(UUID id);
}
