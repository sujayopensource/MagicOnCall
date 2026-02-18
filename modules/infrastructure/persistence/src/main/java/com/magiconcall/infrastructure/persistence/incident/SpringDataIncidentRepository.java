package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.Incident;
import com.magiconcall.domain.incident.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataIncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    List<Incident> findByStatusOrderByCreatedAtDesc(IncidentStatus status);
}
