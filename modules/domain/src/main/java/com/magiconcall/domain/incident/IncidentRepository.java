package com.magiconcall.domain.incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository {

    Incident save(Incident incident);

    Optional<Incident> findById(UUID id);

    Optional<Incident> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);
}
