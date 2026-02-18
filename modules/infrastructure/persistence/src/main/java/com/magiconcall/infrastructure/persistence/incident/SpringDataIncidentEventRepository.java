package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.IncidentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataIncidentEventRepository extends JpaRepository<IncidentEvent, UUID> {

    List<IncidentEvent> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
