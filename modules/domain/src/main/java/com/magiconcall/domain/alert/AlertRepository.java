package com.magiconcall.domain.alert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for alert persistence.
 * Infrastructure layer provides the JPA adapter.
 */
public interface AlertRepository {

    Alert save(Alert alert);

    Optional<Alert> findById(UUID id);

    Optional<Alert> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<Alert> findByIncidentId(UUID incidentId);
}
