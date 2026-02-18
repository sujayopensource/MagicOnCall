package com.magiconcall.application.graph;

import java.util.Map;
import java.util.UUID;

public record AddNodeCommand(
    String nodeType,
    String label,
    String description,
    UUID referenceId,
    String source,
    Map<String, String> metadata
) {}
