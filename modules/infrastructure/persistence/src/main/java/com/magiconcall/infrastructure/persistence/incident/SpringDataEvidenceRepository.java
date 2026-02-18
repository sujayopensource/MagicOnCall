package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataEvidenceRepository extends JpaRepository<Evidence, UUID> {

    List<Evidence> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
