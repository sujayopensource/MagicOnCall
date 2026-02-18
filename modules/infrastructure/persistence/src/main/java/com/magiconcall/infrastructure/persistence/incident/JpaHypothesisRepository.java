package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.Hypothesis;
import com.magiconcall.domain.incident.HypothesisRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaHypothesisRepository implements HypothesisRepository {

    private final SpringDataHypothesisRepository delegate;

    public JpaHypothesisRepository(SpringDataHypothesisRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Hypothesis save(Hypothesis hypothesis) { return delegate.save(hypothesis); }

    @Override
    public List<Hypothesis> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId) {
        return delegate.findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }
}
