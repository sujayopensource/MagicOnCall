package com.magiconcall.infrastructure.persistence.graph;

import com.magiconcall.domain.graph.CorrelationNode;
import com.magiconcall.domain.graph.CorrelationNodeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataCorrelationNodeRepository extends JpaRepository<CorrelationNode, UUID> {

    List<CorrelationNode> findByIncidentId(UUID incidentId);

    List<CorrelationNode> findByIncidentIdAndNodeType(UUID incidentId, CorrelationNodeType nodeType);

    List<CorrelationNode> findByIncidentIdAndReferenceId(UUID incidentId, UUID referenceId);
}
