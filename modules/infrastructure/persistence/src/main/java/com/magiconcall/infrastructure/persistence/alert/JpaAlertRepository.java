package com.magiconcall.infrastructure.persistence.alert;

import com.magiconcall.domain.alert.Alert;
import com.magiconcall.domain.alert.AlertRepository;
import com.magiconcall.domain.alert.AlertStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing the domain AlertRepository port via Spring Data JPA.
 */
@Repository
public class JpaAlertRepository implements AlertRepository {

    private final SpringDataAlertRepository delegate;

    public JpaAlertRepository(SpringDataAlertRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Alert save(Alert alert) {
        return delegate.save(alert);
    }

    @Override
    public Optional<Alert> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public Optional<Alert> findByExternalId(String externalId) {
        return delegate.findByExternalId(externalId);
    }

    @Override
    public boolean existsByExternalId(String externalId) {
        return delegate.existsByExternalId(externalId);
    }

    @Override
    public List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status) {
        return delegate.findByStatusOrderByCreatedAtDesc(status);
    }
}
