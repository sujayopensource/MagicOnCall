package com.magiconcall.api.graph;

import com.magiconcall.application.graph.CorrelationGraphService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}")
public class RootCauseController {

    private final CorrelationGraphService graphService;

    public RootCauseController(CorrelationGraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/root-cause-paths")
    public ResponseEntity<List<RootCausePathResponse>> findRootCausePaths(
            @PathVariable UUID incidentId,
            @RequestParam(defaultValue = "3") int maxPaths) {
        var paths = graphService.findRootCausePaths(incidentId, maxPaths)
            .stream().map(RootCausePathResponse::from).toList();
        return ResponseEntity.ok(paths);
    }

    @GetMapping("/blast-radius/{nodeId}")
    public ResponseEntity<BlastRadiusResponse> computeBlastRadius(
            @PathVariable UUID incidentId,
            @PathVariable UUID nodeId) {
        var result = graphService.computeBlastRadius(incidentId, nodeId);
        return ResponseEntity.ok(BlastRadiusResponse.from(result));
    }
}
