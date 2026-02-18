package com.magiconcall.domain.tool;

import java.util.Map;
import java.util.UUID;

public record ToolRequest(
    UUID incidentId,
    String toolName,
    Map<String, String> parameters,
    String requestedBy
) {}
