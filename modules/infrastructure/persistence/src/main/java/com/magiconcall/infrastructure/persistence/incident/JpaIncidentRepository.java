package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.Incident;
import com.magiconcall.domain.incident.IncidentRepository;
import com.magiconcall.domain.incident.IncidentStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaIncidentRepository implements IncidentRepository {

    private final SpringDataIncidentRepository delegate;

    public JpaIncidentRepository(SpringDataIncidentRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Incident save(Incident incident) { return delegate.save(incident); }

    @Override
    public Optional<Incident> findById(UUID id) { return delegate.findById(id); }

    @Override
    public Optional<Incident> findByExternalId(String externalId) {
        return delegate.findByExternalId(externalId);
    }

    @Override
    public boolean existsByExternalId(String externalId) {
        return delegate.existsByExternalId(externalId);
    }

    @Override
    public List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status) {
        return delegate.findByStatusOrderByCreatedAtDesc(status);
    }
}
