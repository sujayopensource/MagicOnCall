package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.IncidentEvent;
import com.magiconcall.domain.incident.IncidentEventRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaIncidentEventRepository implements IncidentEventRepository {

    private final SpringDataIncidentEventRepository delegate;

    public JpaIncidentEventRepository(SpringDataIncidentEventRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public IncidentEvent save(IncidentEvent event) { return delegate.save(event); }

    @Override
    public List<IncidentEvent> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId) {
        return delegate.findByIncidentIdOrderByCreatedAtAsc(incidentId);
    }
}
