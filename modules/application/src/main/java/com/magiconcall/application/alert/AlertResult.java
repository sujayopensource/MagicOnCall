package com.magiconcall.application.alert;

import com.magiconcall.domain.alert.Alert;

import java.time.Instant;
import java.util.UUID;

public record AlertResult(
    UUID id,
    String externalId,
    String title,
    String description,
    String source,
    String severity,
    String status,
    String policyDecisionReason,
    UUID incidentId,
    String tenantId,
    Instant createdAt
) {
    public static AlertResult from(Alert alert) {
        return new AlertResult(
            alert.getId(),
            alert.getExternalId(),
            alert.getTitle(),
            alert.getDescription(),
            alert.getSource(),
            alert.getSeverity().name(),
            alert.getStatus().name(),
            alert.getPolicyDecisionReason(),
            alert.getIncidentId(),
            alert.getTenantId(),
            alert.getCreatedAt()
        );
    }
}
