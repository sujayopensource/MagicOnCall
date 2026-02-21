package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.Action;
import com.magiconcall.domain.incident.ActionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaActionRepository implements ActionRepository {

    private final SpringDataActionRepository delegate;

    public JpaActionRepository(SpringDataActionRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Action save(Action action) { return delegate.save(action); }

    @Override
    public Optional<Action> findById(UUID id) { return delegate.findById(id); }

    @Override
    public Optional<Action> findByIdempotencyKey(String idempotencyKey) {
        return delegate.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<Action> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId) {
        return delegate.findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }

    @Override
    public List<Action> findByIncidentIdAndStatusOrderByCreatedAtDesc(UUID incidentId, Action.ActionStatus status) {
        return delegate.findByIncidentIdAndStatusOrderByCreatedAtDesc(incidentId, status);
    }
}
