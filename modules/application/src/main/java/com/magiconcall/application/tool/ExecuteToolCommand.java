package com.magiconcall.application.tool;

import java.util.Map;
import java.util.UUID;

public record ExecuteToolCommand(
    String toolName,
    UUID incidentId,
    Map<String, String> parameters,
    String requestedBy
) {}
