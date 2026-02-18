package com.magiconcall.application.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.domain.alert.Alert;
import com.magiconcall.domain.alert.AlertRepository;
import com.magiconcall.domain.alert.AlertSeverity;
import com.magiconcall.domain.event.AlertReceivedEvent;
import com.magiconcall.domain.event.DomainEvent;
import com.magiconcall.domain.event.EventPublisher;
import com.magiconcall.domain.incident.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WebhookIngestionService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestionService.class);

    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventPublisher eventPublisher;
    private final WebhookMetrics metrics;
    private final ObjectMapper objectMapper;

    public WebhookIngestionService(AlertRepository alertRepository,
                                   IncidentRepository incidentRepository,
                                   IncidentEventRepository incidentEventRepository,
                                   AuditLogRepository auditLogRepository,
                                   EventPublisher eventPublisher,
                                   WebhookMetrics metrics,
                                   ObjectMapper objectMapper) {
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.auditLogRepository = auditLogRepository;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WebhookResult ingest(String tenantId, NormalizedAlert normalized) {
        // 1. Dedup check on alert
        var existingAlert = alertRepository.findByExternalId(normalized.dedupKey());
        if (existingAlert.isPresent()) {
            metrics.recordDedupHit();
            log.info("Dedup hit: alert with dedupKey={} already exists, id={}",
                normalized.dedupKey(), existingAlert.get().getId());
            return WebhookResult.deduplicated(
                existingAlert.get().getId(), existingAlert.get().getIncidentId());
        }
        metrics.recordDedupMiss();

        // 2. Find or create incident
        boolean newIncident = false;
        Incident incident;
        var existingIncident = incidentRepository.findByExternalId(normalized.incidentExternalId());
        if (existingIncident.isPresent()) {
            incident = existingIncident.get();
            log.info("Attaching alert to existing incident: incidentId={}, externalId={}",
                incident.getId(), incident.getExternalId());
        } else {
            incident = new Incident(
                normalized.incidentExternalId(),
                normalized.incidentTitle(),
                null,
                IncidentSeverity.valueOf(normalized.incidentSeverity()),
                null,
                "{}"
            );
            incident.setTenantId(tenantId);
            incident = incidentRepository.save(incident);
            newIncident = true;
            metrics.recordIncidentCreated();

            // Timeline: incident created
            var createdEvent = IncidentEvent.created(
                incident.getId(), incident.getTitle(), incident.getSeverity().name());
            createdEvent.setTenantId(tenantId);
            incidentEventRepository.save(createdEvent);

            // Audit log
            var audit = AuditLog.created(incident.getId(), "INCIDENT", incident.getId(),
                "{\"status\":\"NEW\",\"severity\":\"%s\",\"source\":\"pagerduty_webhook\"}"
                    .formatted(incident.getSeverity()));
            audit.setTenantId(tenantId);
            auditLogRepository.save(audit);

            log.info("Incident auto-created from webhook: id={}, externalId={}",
                incident.getId(), incident.getExternalId());
        }

        // 3. Create alert linked to incident
        AlertSeverity alertSeverity = mapSeverity(normalized.severity());
        var alert = new Alert(
            normalized.dedupKey(),
            normalized.title(),
            normalized.description(),
            normalized.source(),
            alertSeverity,
            normalized.labels(),
            "webhook_ingestion"
        );
        alert.setTenantId(tenantId);
        alert.setIncidentId(incident.getId());
        alert = alertRepository.save(alert);
        metrics.recordAlertIngested();

        try (var ignored = MDC.putCloseable("incidentId", incident.getId().toString());
             var ignored2 = MDC.putCloseable("alertId", alert.getId().toString())) {

            log.info("Alert ingested via webhook: alertId={}, dedupKey={}, incidentId={}, newIncident={}",
                alert.getId(), normalized.dedupKey(), incident.getId(), newIncident);

            // 4. Timeline: alert correlated
            var correlationEvent = IncidentEvent.alertCorrelated(
                incident.getId(), alert.getId(), alert.getTitle());
            correlationEvent.setTenantId(tenantId);
            incidentEventRepository.save(correlationEvent);

            // 5. Publish alert.received event via outbox
            publishAlertReceivedEvent(alert, incident.getId(), newIncident);
        }

        return WebhookResult.ingested(alert.getId(), incident.getId(), newIncident);
    }

    private AlertSeverity mapSeverity(String severity) {
        if (severity == null) return AlertSeverity.WARNING;
        return switch (severity.toUpperCase()) {
            case "CRITICAL", "P1" -> AlertSeverity.CRITICAL;
            case "HIGH", "P2" -> AlertSeverity.HIGH;
            case "WARNING", "P3" -> AlertSeverity.WARNING;
            default -> AlertSeverity.INFO;
        };
    }

    private void publishAlertReceivedEvent(Alert alert, UUID incidentId, boolean newIncident) {
        var payload = new AlertReceivedEvent(
            alert.getId(), alert.getExternalId(), alert.getTitle(),
            alert.getSource(), alert.getSeverity().name(),
            incidentId, newIncident
        );
        try {
            String json = objectMapper.writeValueAsString(payload);
            var domainEvent = DomainEvent.of(
                "ALERT_RECEIVED", "Alert", alert.getId(), alert.getTenantId(), json);
            eventPublisher.publish(domainEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize alert.received event", e);
        }
    }

    public record WebhookResult(
        UUID alertId,
        UUID incidentId,
        boolean newIncident,
        boolean deduplicated
    ) {
        public static WebhookResult ingested(UUID alertId, UUID incidentId, boolean newIncident) {
            return new WebhookResult(alertId, incidentId, newIncident, false);
        }

        public static WebhookResult deduplicated(UUID alertId, UUID incidentId) {
            return new WebhookResult(alertId, incidentId, false, true);
        }
    }
}
