package com.magiconcall.infrastructure.persistence.tool;

import com.magiconcall.domain.tool.ToolExecutionLog;
import com.magiconcall.domain.tool.ToolExecutionLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaToolExecutionLogRepository implements ToolExecutionLogRepository {

    private final SpringDataToolExecutionLogRepository delegate;

    public JpaToolExecutionLogRepository(SpringDataToolExecutionLogRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public ToolExecutionLog save(ToolExecutionLog log) { return delegate.save(log); }

    @Override
    public List<ToolExecutionLog> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId) {
        return delegate.findByIncidentIdOrderByCreatedAtDesc(incidentId);
    }

    @Override
    public List<ToolExecutionLog> findByToolNameOrderByCreatedAtDesc(String toolName) {
        return delegate.findByToolNameOrderByCreatedAtDesc(toolName);
    }
}
