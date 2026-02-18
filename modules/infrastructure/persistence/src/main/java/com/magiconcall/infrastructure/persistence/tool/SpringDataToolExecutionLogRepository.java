package com.magiconcall.infrastructure.persistence.tool;

import com.magiconcall.domain.tool.ToolExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SpringDataToolExecutionLogRepository extends JpaRepository<ToolExecutionLog, UUID> {

    List<ToolExecutionLog> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);

    List<ToolExecutionLog> findByToolNameOrderByCreatedAtDesc(String toolName);
}
