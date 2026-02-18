package com.magiconcall.api.graph;

import com.magiconcall.application.graph.AddEdgeCommand;
import com.magiconcall.application.graph.AddNodeCommand;
import com.magiconcall.application.graph.CorrelationGraphService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/graph")
public class CorrelationGraphController {

    private final CorrelationGraphService graphService;

    public CorrelationGraphController(CorrelationGraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping
    public ResponseEntity<CorrelationGraphResponse> getGraph(@PathVariable UUID incidentId) {
        var result = graphService.getGraph(incidentId);
        return ResponseEntity.ok(CorrelationGraphResponse.from(result));
    }

    @PostMapping("/nodes")
    public ResponseEntity<GraphNodeResponse> addNode(
            @PathVariable UUID incidentId,
            @Valid @RequestBody AddNodeRequest request) {
        var command = new AddNodeCommand(
            request.nodeType(), request.label(), request.description(),
            request.referenceId(), request.source(), request.metadata()
        );
        var result = graphService.addNode(incidentId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(GraphNodeResponse.from(result));
    }

    @PostMapping("/edges")
    public ResponseEntity<GraphEdgeResponse> addEdge(
            @PathVariable UUID incidentId,
            @Valid @RequestBody AddEdgeRequest request) {
        var command = new AddEdgeCommand(
            request.sourceNodeId(), request.targetNodeId(), request.edgeType(),
            request.weight(), request.reason(), request.metadata()
        );
        var result = graphService.addEdge(incidentId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(GraphEdgeResponse.from(result));
    }
}
