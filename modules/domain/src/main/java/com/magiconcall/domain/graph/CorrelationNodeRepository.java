package com.magiconcall.domain.graph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorrelationNodeRepository {

    CorrelationNode save(CorrelationNode node);

    Optional<CorrelationNode> findById(UUID id);

    List<CorrelationNode> findByIncidentId(UUID incidentId);

    List<CorrelationNode> findByIncidentIdAndNodeType(UUID incidentId, CorrelationNodeType nodeType);

    List<CorrelationNode> findByIncidentIdAndReferenceId(UUID incidentId, UUID referenceId);

    void deleteById(UUID id);
}
