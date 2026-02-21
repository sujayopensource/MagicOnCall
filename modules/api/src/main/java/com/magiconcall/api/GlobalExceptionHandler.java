package com.magiconcall.api;

import com.magiconcall.application.action.ActionService;
import com.magiconcall.application.alert.AlertService;
import com.magiconcall.application.graph.CorrelationGraphService;
import com.magiconcall.application.incident.IncidentService;
import com.magiconcall.application.tool.ToolRegistry;
import com.magiconcall.application.triage.TokenBudgetExceededException;
import com.magiconcall.domain.incident.Action;
import com.magiconcall.domain.incident.IncidentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AlertService.AlertNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAlertNotFound(AlertService.AlertNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorBody("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AlertService.AlertDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAlertDenied(AlertService.AlertDeniedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(errorBody("POLICY_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(IncidentService.IncidentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleIncidentNotFound(IncidentService.IncidentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorBody("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(IncidentStatus.InvalidTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTransition(IncidentStatus.InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(errorBody("INVALID_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(ToolRegistry.ToolNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleToolNotFound(ToolRegistry.ToolNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorBody("TOOL_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(CorrelationGraphService.NodeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNodeNotFound(CorrelationGraphService.NodeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorBody("NODE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ActionService.ActionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleActionNotFound(ActionService.ActionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(errorBody("ACTION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(Action.InvalidActionTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidActionTransition(Action.InvalidActionTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(errorBody("INVALID_ACTION_TRANSITION", ex.getMessage()));
    }

    @ExceptionHandler(TokenBudgetExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTokenBudgetExceeded(TokenBudgetExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(errorBody("TOKEN_BUDGET_EXCEEDED", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "VALIDATION_ERROR",
                "message", "Request validation failed",
                "details", errors,
                "timestamp", Instant.now().toString()
            ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(errorBody("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorBody("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private Map<String, Object> errorBody(String error, String message) {
        return Map.of(
            "error", error,
            "message", message,
            "timestamp", Instant.now().toString()
        );
    }
}
