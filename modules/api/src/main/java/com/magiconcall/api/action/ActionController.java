package com.magiconcall.api.action;

import com.magiconcall.application.action.ActionService;
import com.magiconcall.application.action.ApproveActionCommand;
import com.magiconcall.application.action.ProposeActionCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/actions")
public class ActionController {

    private final ActionService actionService;

    public ActionController(ActionService actionService) {
        this.actionService = actionService;
    }

    @PostMapping("/propose")
    public ResponseEntity<ActionResponse> propose(
            @PathVariable UUID incidentId,
            @Valid @RequestBody ProposeActionRequest request) {
        var command = new ProposeActionCommand(
            request.title(), request.description(), request.toolName(),
            request.parameters(), request.riskLevel(),
            request.proposedBy(), request.idempotencyKey()
        );
        var result = actionService.propose(incidentId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ActionResponse.from(result));
    }

    @PostMapping("/{actionId}/approve")
    public ResponseEntity<ActionResponse> approve(
            @PathVariable UUID incidentId,
            @PathVariable UUID actionId,
            @Valid @RequestBody ApproveActionRequest request) {
        var result = actionService.approve(incidentId, actionId, new ApproveActionCommand(request.approvedBy()));
        return ResponseEntity.ok(ActionResponse.from(result));
    }

    @PostMapping("/{actionId}/reject")
    public ResponseEntity<ActionResponse> reject(
            @PathVariable UUID incidentId,
            @PathVariable UUID actionId,
            @Valid @RequestBody RejectActionRequest request) {
        var result = actionService.reject(incidentId, actionId, request.rejectedBy(), request.reason());
        return ResponseEntity.ok(ActionResponse.from(result));
    }

    @PostMapping("/{actionId}/execute")
    public ResponseEntity<ActionResponse> execute(
            @PathVariable UUID incidentId,
            @PathVariable UUID actionId) {
        var result = actionService.execute(incidentId, actionId);
        return ResponseEntity.ok(ActionResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<List<ActionResponse>> listActions(@PathVariable UUID incidentId) {
        var results = actionService.listActions(incidentId);
        return ResponseEntity.ok(results.stream().map(ActionResponse::from).toList());
    }

    @GetMapping("/{actionId}")
    public ResponseEntity<ActionResponse> getAction(
            @PathVariable UUID incidentId,
            @PathVariable UUID actionId) {
        var result = actionService.findAction(incidentId, actionId);
        return ResponseEntity.ok(ActionResponse.from(result));
    }
}
