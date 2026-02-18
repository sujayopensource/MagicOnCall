package com.magiconcall.infrastructure.persistence.graph;

import com.magiconcall.domain.graph.CorrelationEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataCorrelationEdgeRepository extends JpaRepository<CorrelationEdge, UUID> {

    List<CorrelationEdge> findByIncidentId(UUID incidentId);

    List<CorrelationEdge> findBySourceNodeId(UUID sourceNodeId);

    List<CorrelationEdge> findByTargetNodeId(UUID targetNodeId);
}
