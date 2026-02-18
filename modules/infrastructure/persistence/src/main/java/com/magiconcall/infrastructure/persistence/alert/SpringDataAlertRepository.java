package com.magiconcall.infrastructure.persistence.alert;

import com.magiconcall.domain.alert.Alert;
import com.magiconcall.domain.alert.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataAlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<Alert> findByIncidentId(UUID incidentId);
}
