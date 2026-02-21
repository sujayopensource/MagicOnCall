package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.Action;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataActionRepository extends JpaRepository<Action, UUID> {

    Optional<Action> findByIdempotencyKey(String idempotencyKey);

    List<Action> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);

    List<Action> findByIncidentIdAndStatusOrderByCreatedAtDesc(UUID incidentId, Action.ActionStatus status);
}
