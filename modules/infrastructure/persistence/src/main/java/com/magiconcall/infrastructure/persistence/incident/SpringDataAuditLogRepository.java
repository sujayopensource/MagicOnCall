package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataAuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
