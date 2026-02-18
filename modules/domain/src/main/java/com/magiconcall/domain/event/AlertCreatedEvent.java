package com.magiconcall.domain.event;

import java.util.UUID;

public record AlertCreatedEvent(
    UUID alertId,
    String externalId,
    String title,
    String source,
    String severity,
    String status,
    String policyDecisionReason
) {}
