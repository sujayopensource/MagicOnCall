package com.magiconcall.domain.tool;

import com.magiconcall.domain.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tool_execution_log")
public class ToolExecutionLog extends BaseEntity {

    @Column(nullable = false)
    private String toolName;

    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ToolExecutionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String requestPayload;

    @Column(columnDefinition = "text")
    private String responseContent;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false)
    private long durationMs;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private String requestedBy;

    @Column(nullable = false)
    private Instant executedAt;

    protected ToolExecutionLog() {}

    public ToolExecutionLog(String toolName, UUID incidentId, ToolExecutionStatus status,
                            String requestPayload, String responseContent, String errorMessage,
                            long durationMs, int retryCount, String requestedBy, Instant executedAt) {
        this.toolName = toolName;
        this.incidentId = incidentId;
        this.status = status;
        this.requestPayload = requestPayload;
        this.responseContent = responseContent;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
        this.retryCount = retryCount;
        this.requestedBy = requestedBy;
        this.executedAt = executedAt;
    }

    public String getToolName() { return toolName; }
    public UUID getIncidentId() { return incidentId; }
    public ToolExecutionStatus getStatus() { return status; }
    public String getRequestPayload() { return requestPayload; }
    public String getResponseContent() { return responseContent; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
    public int getRetryCount() { return retryCount; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getExecutedAt() { return executedAt; }
}
