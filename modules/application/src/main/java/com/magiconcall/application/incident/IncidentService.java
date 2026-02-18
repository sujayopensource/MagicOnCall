package com.magiconcall.application.incident;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magiconcall.domain.event.DomainEvent;
import com.magiconcall.domain.event.EventPublisher;
import com.magiconcall.domain.event.IncidentCreatedEvent;
import com.magiconcall.domain.incident.*;
import com.magiconcall.domain.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final HypothesisRepository hypothesisRepository;
    private final EvidenceRepository evidenceRepository;
    private final AuditLogRepository auditLogRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentEventRepository incidentEventRepository,
                           HypothesisRepository hypothesisRepository,
                           EvidenceRepository evidenceRepository,
                           AuditLogRepository auditLogRepository,
                           EventPublisher eventPublisher,
                           ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.evidenceRepository = evidenceRepository;
        this.auditLogRepository = auditLogRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IncidentResult create(CreateIncidentCommand command) {
        if (incidentRepository.existsByExternalId(command.externalId())) {
            log.info("Idempotent hit: incident with externalId={} already exists", command.externalId());
            return incidentRepository.findByExternalId(command.externalId())
                .map(IncidentResult::from)
                .orElseThrow();
        }

        var severity = IncidentSeverity.valueOf(command.severity().toUpperCase());
        String tagsJson = serializeMap(command.tags());

        var incident = new Incident(
            command.externalId(), command.title(), command.summary(),
            severity, command.commanderName(), tagsJson
        );
        incident.setTenantId(TenantContext.requireTenantId());
        incident = incidentRepository.save(incident);

        try (var ignored = MDC.putCloseable("incidentId", incident.getId().toString())) {
            log.info("Incident created: id={}, title={}, severity={}, commander={}",
                incident.getId(), incident.getTitle(), severity, command.commanderName());

            // Timeline event
            var timelineEvent = IncidentEvent.created(
                incident.getId(), incident.getTitle(), severity.name());
            timelineEvent.setTenantId(incident.getTenantId());
            incidentEventRepository.save(timelineEvent);

            // Audit log
            var audit = AuditLog.created(incident.getId(), "INCIDENT", incident.getId(),
                "{\"status\":\"NEW\",\"severity\":\"%s\"}".formatted(severity));
            audit.setTenantId(incident.getTenantId());
            auditLogRepository.save(audit);

            // Outbox event
            publishIncidentCreatedEvent(incident);
        }

        return IncidentResult.from(incident);
    }

    @Transactional(readOnly = true)
    public Optional<IncidentResult> findById(UUID id) {
        return incidentRepository.findById(id).map(IncidentResult::from);
    }

    @Transactional
    public IncidentResult transition(UUID id, IncidentStatus newStatus) {
        var incident = incidentRepository.findById(id)
            .orElseThrow(() -> new IncidentNotFoundException(id));

        try (var ignored = MDC.putCloseable("incidentId", id.toString())) {
            IncidentStatus previousStatus = incident.transitionTo(newStatus);
            incident = incidentRepository.save(incident);

            log.info("Incident transitioned: {} → {}", previousStatus, newStatus);

            // Timeline event
            var timelineEvent = IncidentEvent.statusChanged(id, previousStatus, newStatus);
            timelineEvent.setTenantId(incident.getTenantId());
            incidentEventRepository.save(timelineEvent);

            // Audit log
            var audit = AuditLog.statusChanged(id, "INCIDENT", id,
                "{\"status\":\"%s\"}".formatted(previousStatus),
                "{\"status\":\"%s\"}".formatted(newStatus));
            audit.setTenantId(incident.getTenantId());
            auditLogRepository.save(audit);
        }

        return IncidentResult.from(incident);
    }

    @Transactional(readOnly = true)
    public List<TimelineResult> getTimeline(UUID incidentId) {
        if (incidentRepository.findById(incidentId).isEmpty()) {
            throw new IncidentNotFoundException(incidentId);
        }
        return incidentEventRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId)
            .stream().map(TimelineResult::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HypothesisResult> getHypotheses(UUID incidentId) {
        if (incidentRepository.findById(incidentId).isEmpty()) {
            throw new IncidentNotFoundException(incidentId);
        }
        return hypothesisRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
            .stream().map(HypothesisResult::from).toList();
    }

    @Transactional
    public HypothesisResult addHypothesis(UUID incidentId, AddHypothesisCommand command) {
        var incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            var hypothesis = new Hypothesis(
                incidentId, command.title(), command.description(),
                command.confidence(), command.source()
            );
            hypothesis.setEvidenceFor(command.evidenceFor());
            hypothesis.setEvidenceAgainst(command.evidenceAgainst());
            hypothesis.setNextBestTest(command.nextBestTest());
            hypothesis.setStopCondition(command.stopCondition());
            hypothesis.setTenantId(incident.getTenantId());
            hypothesis = hypothesisRepository.save(hypothesis);

            log.info("Hypothesis added: id={}, title={}, source={}",
                hypothesis.getId(), hypothesis.getTitle(), hypothesis.getSource());

            // Timeline event
            var timelineEvent = IncidentEvent.hypothesisAdded(incidentId, command.title());
            timelineEvent.setTenantId(incident.getTenantId());
            incidentEventRepository.save(timelineEvent);

            // Audit log
            var audit = AuditLog.created(incidentId, "HYPOTHESIS", hypothesis.getId(),
                "{\"title\":\"%s\",\"source\":\"%s\"}".formatted(command.title(), command.source()));
            audit.setTenantId(incident.getTenantId());
            auditLogRepository.save(audit);

            return HypothesisResult.from(hypothesis);
        }
    }

    @Transactional
    public EvidenceResult addEvidence(UUID incidentId, AddEvidenceCommand command) {
        var incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        try (var ignored = MDC.putCloseable("incidentId", incidentId.toString())) {
            var evidenceType = EvidenceType.valueOf(command.evidenceType().toUpperCase());

            var evidence = new Evidence(
                incidentId, command.hypothesisId(), evidenceType,
                command.title(), command.content(), command.sourceUrl(), "{}"
            );
            evidence.setTenantId(incident.getTenantId());
            evidence = evidenceRepository.save(evidence);

            log.info("Evidence added: id={}, type={}, title={}",
                evidence.getId(), evidenceType, evidence.getTitle());

            // Timeline event
            var timelineEvent = IncidentEvent.evidenceAdded(
                incidentId, command.title(), evidenceType.name());
            timelineEvent.setTenantId(incident.getTenantId());
            incidentEventRepository.save(timelineEvent);

            // Audit log
            var audit = AuditLog.created(incidentId, "EVIDENCE", evidence.getId(),
                "{\"type\":\"%s\",\"title\":\"%s\"}".formatted(evidenceType, command.title()));
            audit.setTenantId(incident.getTenantId());
            auditLogRepository.save(audit);

            return EvidenceResult.from(evidence);
        }
    }

    private void publishIncidentCreatedEvent(Incident incident) {
        var payload = new IncidentCreatedEvent(
            incident.getId(), incident.getExternalId(), incident.getTitle(),
            incident.getSeverity().name(), incident.getStatus().name(),
            incident.getCommanderName()
        );
        try {
            String json = objectMapper.writeValueAsString(payload);
            var domainEvent = DomainEvent.of(
                "INCIDENT_CREATED", "Incident", incident.getId(),
                incident.getTenantId(), json);
            eventPublisher.publish(domainEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize incident event", e);
        }
    }

    private String serializeMap(java.util.Map<String, String> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public static class IncidentNotFoundException extends RuntimeException {
        public IncidentNotFoundException(UUID id) {
            super("Incident not found: " + id);
        }
    }
}
