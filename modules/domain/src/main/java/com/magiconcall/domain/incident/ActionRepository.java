package com.magiconcall.domain.incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActionRepository {

    Action save(Action action);

    Optional<Action> findById(UUID id);

    Optional<Action> findByIdempotencyKey(String idempotencyKey);

    List<Action> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);

    List<Action> findByIncidentIdAndStatusOrderByCreatedAtDesc(UUID incidentId, Action.ActionStatus status);
}
