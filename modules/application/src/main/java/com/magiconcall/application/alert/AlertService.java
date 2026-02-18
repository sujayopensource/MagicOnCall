package com.magiconcall.application.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.domain.alert.Alert;
import com.magiconcall.domain.alert.AlertRepository;
import com.magiconcall.domain.alert.AlertSeverity;
import com.magiconcall.domain.alert.AlertStatus;
import com.magiconcall.domain.event.AlertCreatedEvent;
import com.magiconcall.domain.event.DomainEvent;
import com.magiconcall.domain.event.EventPublisher;
import com.magiconcall.domain.policy.AlertPolicyEvaluator;
import com.magiconcall.domain.policy.PolicyContext;
import com.magiconcall.domain.policy.PolicyDecision;
import com.magiconcall.domain.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final EventPublisher eventPublisher;
    private final AlertPolicyEvaluator policyEvaluator;
    private final ObjectMapper objectMapper;

    public AlertService(AlertRepository alertRepository,
                        EventPublisher eventPublisher,
                        AlertPolicyEvaluator policyEvaluator,
                        ObjectMapper objectMapper) {
        this.alertRepository = alertRepository;
        this.eventPublisher = eventPublisher;
        this.policyEvaluator = policyEvaluator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AlertResult ingest(IngestAlertCommand command) {
        boolean isDuplicate = alertRepository.existsByExternalId(command.externalId());

        var policyContext = PolicyContext.builder("ingest_alert", "Alert")
            .attribute("externalId", command.externalId())
            .attribute("severity", command.severity())
            .attribute("source", command.source())
            .attribute("isDuplicate", isDuplicate)
            .build();

        PolicyDecision decision = policyEvaluator.evaluate(policyContext);

        if (!decision.isAllowed()) {
            log.info("Alert ingestion denied: externalId={}, reason={}",
                command.externalId(), decision.reason());
            throw new AlertDeniedException(decision.reason());
        }

        AlertSeverity severity = AlertSeverity.valueOf(command.severity().toUpperCase());
        String labelsJson = serializeLabels(command);

        var alert = new Alert(
            command.externalId(),
            command.title(),
            command.description(),
            command.source(),
            severity,
            labelsJson,
            decision.reason()
        );
        alert.setTenantId(TenantContext.requireTenantId());

        alert = alertRepository.save(alert);
        log.info("Alert ingested: id={}, externalId={}, severity={}, tenant={}",
            alert.getId(), alert.getExternalId(), alert.getSeverity(), alert.getTenantId());

        publishAlertCreatedEvent(alert);

        return AlertResult.from(alert);
    }

    @Transactional(readOnly = true)
    public Optional<AlertResult> findById(UUID id) {
        return alertRepository.findById(id).map(AlertResult::from);
    }

    @Transactional(readOnly = true)
    public Optional<AlertResult> findByExternalId(String externalId) {
        return alertRepository.findByExternalId(externalId).map(AlertResult::from);
    }

    @Transactional(readOnly = true)
    public List<AlertResult> findByStatus(AlertStatus status) {
        return alertRepository.findByStatusOrderByCreatedAtDesc(status)
            .stream().map(AlertResult::from).toList();
    }

    @Transactional
    public AlertResult acknowledge(UUID id) {
        var alert = alertRepository.findById(id)
            .orElseThrow(() -> new AlertNotFoundException(id));
        alert.acknowledge();
        return AlertResult.from(alertRepository.save(alert));
    }

    @Transactional
    public AlertResult resolve(UUID id) {
        var alert = alertRepository.findById(id)
            .orElseThrow(() -> new AlertNotFoundException(id));
        alert.resolve();
        return AlertResult.from(alertRepository.save(alert));
    }

    private void publishAlertCreatedEvent(Alert alert) {
        var eventPayload = new AlertCreatedEvent(
            alert.getId(),
            alert.getExternalId(),
            alert.getTitle(),
            alert.getSource(),
            alert.getSeverity().name(),
            alert.getStatus().name(),
            alert.getPolicyDecisionReason()
        );

        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            var domainEvent = DomainEvent.of(
                "ALERT_CREATED", "Alert", alert.getId(), alert.getTenantId(), payload);
            eventPublisher.publish(domainEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize alert event", e);
        }
    }

    private String serializeLabels(IngestAlertCommand command) {
        if (command.labels() == null || command.labels().isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(command.labels());
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public static class AlertNotFoundException extends RuntimeException {
        public AlertNotFoundException(UUID id) {
            super("Alert not found: " + id);
        }
    }

    public static class AlertDeniedException extends RuntimeException {
        public AlertDeniedException(String reason) {
            super("Alert denied by policy: " + reason);
        }
    }
}
