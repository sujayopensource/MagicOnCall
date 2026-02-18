package com.magiconcall.domain.incident;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
