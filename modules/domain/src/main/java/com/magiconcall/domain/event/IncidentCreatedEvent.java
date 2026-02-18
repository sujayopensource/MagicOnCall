package com.magiconcall.domain.event;

import java.util.UUID;

public record IncidentCreatedEvent(
    UUID incidentId,
    String externalId,
    String title,
    String severity,
    String status,
    String commanderName
) {}
