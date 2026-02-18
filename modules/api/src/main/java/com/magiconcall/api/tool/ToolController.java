package com.magiconcall.api.tool;

import com.magiconcall.application.tool.ExecuteToolCommand;
import com.magiconcall.application.tool.ToolExecutionService;
import com.magiconcall.application.tool.ToolRegistry;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final ToolExecutionService toolExecutionService;
    private final ToolRegistry toolRegistry;

    public ToolController(ToolExecutionService toolExecutionService,
                          ToolRegistry toolRegistry) {
        this.toolExecutionService = toolExecutionService;
        this.toolRegistry = toolRegistry;
    }

    @PostMapping("/{toolName}/run")
    public ResponseEntity<ToolRunResponse> runTool(
            @PathVariable String toolName,
            @Valid @RequestBody ToolRunRequest request) {
        var command = new ExecuteToolCommand(
            toolName, request.incidentId(),
            request.parameters() != null ? request.parameters() : Map.of(),
            request.requestedBy()
        );
        var result = toolExecutionService.executeTool(command);
        return ResponseEntity.ok(ToolRunResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<List<String>> listTools() {
        return ResponseEntity.ok(toolRegistry.availableTools());
    }
}
