package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.Hypothesis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataHypothesisRepository extends JpaRepository<Hypothesis, UUID> {

    List<Hypothesis> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
