package com.magiconcall.infrastructure.persistence.graph;

import com.magiconcall.domain.graph.CorrelationNode;
import com.magiconcall.domain.graph.CorrelationNodeRepository;
import com.magiconcall.domain.graph.CorrelationNodeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCorrelationNodeRepository implements CorrelationNodeRepository {

    private final SpringDataCorrelationNodeRepository delegate;

    public JpaCorrelationNodeRepository(SpringDataCorrelationNodeRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public CorrelationNode save(CorrelationNode node) {
        return delegate.save(node);
    }

    @Override
    public Optional<CorrelationNode> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public List<CorrelationNode> findByIncidentId(UUID incidentId) {
        return delegate.findByIncidentId(incidentId);
    }

    @Override
    public List<CorrelationNode> findByIncidentIdAndNodeType(UUID incidentId, CorrelationNodeType nodeType) {
        return delegate.findByIncidentIdAndNodeType(incidentId, nodeType);
    }

    @Override
    public List<CorrelationNode> findByIncidentIdAndReferenceId(UUID incidentId, UUID referenceId) {
        return delegate.findByIncidentIdAndReferenceId(incidentId, referenceId);
    }

    @Override
    public void deleteById(UUID id) {
        delegate.deleteById(id);
    }
}
