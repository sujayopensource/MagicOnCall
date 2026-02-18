package com.magiconcall.infrastructure.persistence.graph;

import com.magiconcall.domain.graph.CorrelationEdge;
import com.magiconcall.domain.graph.CorrelationEdgeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCorrelationEdgeRepository implements CorrelationEdgeRepository {

    private final SpringDataCorrelationEdgeRepository delegate;

    public JpaCorrelationEdgeRepository(SpringDataCorrelationEdgeRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public CorrelationEdge save(CorrelationEdge edge) {
        return delegate.save(edge);
    }

    @Override
    public Optional<CorrelationEdge> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public List<CorrelationEdge> findByIncidentId(UUID incidentId) {
        return delegate.findByIncidentId(incidentId);
    }

    @Override
    public List<CorrelationEdge> findBySourceNodeId(UUID sourceNodeId) {
        return delegate.findBySourceNodeId(sourceNodeId);
    }

    @Override
    public List<CorrelationEdge> findByTargetNodeId(UUID targetNodeId) {
        return delegate.findByTargetNodeId(targetNodeId);
    }

    @Override
    public void deleteById(UUID id) {
        delegate.deleteById(id);
    }
}
