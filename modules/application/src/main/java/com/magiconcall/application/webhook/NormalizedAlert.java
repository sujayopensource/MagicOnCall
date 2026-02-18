package com.magiconcall.application.webhook;

/**
 * Source-agnostic alert representation after normalization from a webhook payload.
 */
public record NormalizedAlert(
    String dedupKey,
    String title,
    String description,
    String source,
    String severity,
    String incidentExternalId,
    String incidentTitle,
    String incidentSeverity,
    String labels
) {}
