package com.magiconcall.application.alert;

import java.util.Map;

public record IngestAlertCommand(
    String externalId,
    String title,
    String description,
    String source,
    String severity,
    Map<String, String> labels
) {}
