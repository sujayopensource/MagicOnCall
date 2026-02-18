package com.magiconcall.application.incident;

import java.util.Map;

public record CreateIncidentCommand(
    String externalId,
    String title,
    String summary,
    String severity,
    String commanderName,
    Map<String, String> tags
) {}
