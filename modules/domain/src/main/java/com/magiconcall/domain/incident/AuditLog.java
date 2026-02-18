package com.magiconcall.domain.incident;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

    public enum AuditAction { CREATED, UPDATED, STATUS_CHANGED }

    @Column(nullable = false)
    private UUID incidentId;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String previousState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String newState;

    protected AuditLog() {}

    public AuditLog(UUID incidentId, String entityType, UUID entityId,
                    AuditAction action, String previousState, String newState) {
        this.incidentId = incidentId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
    }

    public static AuditLog created(UUID incidentId, String entityType, UUID entityId, String state) {
        return new AuditLog(incidentId, entityType, entityId, AuditAction.CREATED, null, state);
    }

    public static AuditLog statusChanged(UUID incidentId, String entityType, UUID entityId,
                                          String previousState, String newState) {
        return new AuditLog(incidentId, entityType, entityId,
            AuditAction.STATUS_CHANGED, previousState, newState);
    }

    public UUID getIncidentId() { return incidentId; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public AuditAction getAction() { return action; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
}
