package com.magiconcall.application.action;

import java.util.Map;

public record ProposeActionCommand(
    String title,
    String description,
    String toolName,
    Map<String, String> parameters,
    String riskLevel,
    String proposedBy,
    String idempotencyKey
) {}
