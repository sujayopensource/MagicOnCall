package com.magiconcall.api.incident;

import com.magiconcall.application.incident.*;
import com.magiconcall.domain.incident.IncidentStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request) {
        var command = new CreateIncidentCommand(
            request.externalId(), request.title(), request.summary(),
            request.severity(), request.commanderName(), request.tags()
        );
        var result = incidentService.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getById(@PathVariable UUID id) {
        return incidentService.findById(id)
            .map(IncidentResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<TimelineResponse>> getTimeline(@PathVariable UUID id) {
        var timeline = incidentService.getTimeline(id)
            .stream().map(TimelineResponse::from).toList();
        return ResponseEntity.ok(timeline);
    }

    @PostMapping("/{id}/hypotheses")
    public ResponseEntity<HypothesisResponse> addHypothesis(
            @PathVariable UUID id,
            @Valid @RequestBody AddHypothesisRequest request) {
        var command = new AddHypothesisCommand(
            request.title(), request.description(),
            request.confidence(), request.source()
        );
        var result = incidentService.addHypothesis(id, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(HypothesisResponse.from(result));
    }

    @PostMapping("/{id}/evidence")
    public ResponseEntity<EvidenceResponse> addEvidence(
            @PathVariable UUID id,
            @Valid @RequestBody AddEvidenceRequest request) {
        var command = new AddEvidenceCommand(
            request.hypothesisId(), request.evidenceType(),
            request.title(), request.content(), request.sourceUrl()
        );
        var result = incidentService.addEvidence(id, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(EvidenceResponse.from(result));
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<IncidentResponse> transition(
            @PathVariable UUID id,
            @RequestParam String status) {
        var newStatus = IncidentStatus.valueOf(status.toUpperCase());
        var result = incidentService.transition(id, newStatus);
        return ResponseEntity.ok(IncidentResponse.from(result));
    }
}
