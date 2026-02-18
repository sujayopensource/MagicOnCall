package com.magiconcall.domain.tool;

import java.util.List;
import java.util.UUID;

public interface ToolExecutionLogRepository {

    ToolExecutionLog save(ToolExecutionLog log);

    List<ToolExecutionLog> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);

    List<ToolExecutionLog> findByToolNameOrderByCreatedAtDesc(String toolName);
}
