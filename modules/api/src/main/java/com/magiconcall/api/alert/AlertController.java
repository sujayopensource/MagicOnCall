package com.magiconcall.api.alert;

import com.magiconcall.application.alert.AlertService;
import com.magiconcall.application.alert.IngestAlertCommand;
import com.magiconcall.domain.alert.AlertStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    public ResponseEntity<AlertResponse> ingest(@Valid @RequestBody AlertRequest request) {
        var command = new IngestAlertCommand(
            request.externalId(),
            request.title(),
            request.description(),
            request.source(),
            request.severity(),
            request.labels()
        );
        var result = alertService.ingest(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(AlertResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getById(@PathVariable UUID id) {
        return alertService.findById(id)
            .map(AlertResponse::from)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> listByStatus(
            @RequestParam(defaultValue = "OPEN") String status) {
        var results = alertService.findByStatus(AlertStatus.valueOf(status.toUpperCase()));
        var response = results.stream().map(AlertResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable UUID id) {
        var result = alertService.acknowledge(id);
        return ResponseEntity.ok(AlertResponse.from(result));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertResponse> resolve(@PathVariable UUID id) {
        var result = alertService.resolve(id);
        return ResponseEntity.ok(AlertResponse.from(result));
    }
}
