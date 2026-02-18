package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.Evidence;
import com.magiconcall.domain.incident.EvidenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaEvidenceRepository implements EvidenceRepository {

    private final SpringDataEvidenceRepository delegate;

    public JpaEvidenceRepository(SpringDataEvidenceRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Evidence save(Evidence evidence) { return delegate.save(evidence); }

    @Override
    public List<Evidence> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId) {
        return delegate.findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }
}
