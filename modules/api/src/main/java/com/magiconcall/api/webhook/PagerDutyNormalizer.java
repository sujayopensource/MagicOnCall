package com.magiconcall.api.webhook;

import com.magiconcall.application.webhook.NormalizedAlert;
import org.springframework.stereotype.Component;

/**
 * Converts PagerDuty webhook payloads into source-agnostic NormalizedAlerts.
 */
@Component
public class PagerDutyNormalizer {

    public NormalizedAlert normalize(PagerDutyPayload.Message message) {
        var incident = message.incident();
        var service = incident.service();

        String dedupKey = buildDedupKey(service, incident);
        String incidentExternalId = "pd-incident:" + incident.id();
        String severity = mapUrgencyToSeverity(incident.urgency());

        String labels = buildLabels(service, incident, message.event());

        return new NormalizedAlert(
            dedupKey,
            incident.title(),
            incident.description(),
            "pagerduty",
            severity,
            incidentExternalId,
            "[PD] " + incident.title(),
            mapUrgencyToIncidentSeverity(incident.urgency()),
            labels
        );
    }

    private String buildDedupKey(PagerDutyPayload.Service service,
                                  PagerDutyPayload.Incident incident) {
        String serviceId = service != null ? service.id() : "unknown";
        String incidentKey = incident.incidentKey() != null
            ? incident.incidentKey()
            : incident.id();
        return "pd:%s:%s".formatted(serviceId, incidentKey);
    }

    private String mapUrgencyToSeverity(String urgency) {
        if (urgency == null) return "WARNING";
        return switch (urgency.toLowerCase()) {
            case "high" -> "CRITICAL";
            case "low" -> "WARNING";
            default -> "WARNING";
        };
    }

    private String mapUrgencyToIncidentSeverity(String urgency) {
        if (urgency == null) return "SEV3";
        return switch (urgency.toLowerCase()) {
            case "high" -> "SEV1";
            case "low" -> "SEV3";
            default -> "SEV3";
        };
    }

    private String buildLabels(PagerDutyPayload.Service service,
                               PagerDutyPayload.Incident incident,
                               String event) {
        String serviceId = service != null ? service.id() : "";
        String serviceName = service != null ? service.name() : "";
        return """
            {"source":"pagerduty","pd_service_id":"%s","pd_service_name":"%s","pd_incident_id":"%s","pd_event":"%s"}"""
            .formatted(serviceId, serviceName, incident.id(), event != null ? event : "")
            .strip();
    }
}
