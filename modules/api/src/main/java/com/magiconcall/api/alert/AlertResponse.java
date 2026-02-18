package com.magiconcall.api.alert;

import com.magiconcall.application.alert.AlertResult;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
    UUID id,
    String externalId,
    String title,
    String description,
    String source,
    String severity,
    String status,
    String policyDecisionReason,
    Instant createdAt
) {
    public static AlertResponse from(AlertResult result) {
        return new AlertResponse(
            result.id(),
            result.externalId(),
            result.title(),
            result.description(),
            result.source(),
            result.severity(),
            result.status(),
            result.policyDecisionReason(),
            result.createdAt()
        );
    }
}
