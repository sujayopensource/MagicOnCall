package com.magiconcall.domain.event;

import java.util.UUID;

public record AlertReceivedEvent(
    UUID alertId,
    String externalId,
    String title,
    String source,
    String severity,
    UUID incidentId,
    boolean newIncident
) {}
