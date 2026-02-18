package com.magiconcall.infrastructure.persistence.incident;

import com.magiconcall.domain.incident.AuditLog;
import com.magiconcall.domain.incident.AuditLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaAuditLogRepository implements AuditLogRepository {

    private final SpringDataAuditLogRepository delegate;

    public JpaAuditLogRepository(SpringDataAuditLogRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public AuditLog save(AuditLog auditLog) { return delegate.save(auditLog); }

    @Override
    public List<AuditLog> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId) {
        return delegate.findByIncidentIdOrderByCreatedAtAsc(incidentId);
    }
}
