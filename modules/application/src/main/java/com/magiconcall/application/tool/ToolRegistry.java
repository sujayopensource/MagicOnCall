package com.magiconcall.application.tool;

import com.magiconcall.domain.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> toolList) {
        toolList.forEach(tool -> tools.put(tool.name(), tool));
    }

    public Optional<Tool> findByName(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Tool getByName(String name) {
        return findByName(name)
            .orElseThrow(() -> new ToolNotFoundException("Tool not found: " + name));
    }

    public List<String> availableTools() {
        return List.copyOf(tools.keySet());
    }

    public static class ToolNotFoundException extends RuntimeException {
        public ToolNotFoundException(String message) {
            super(message);
        }
    }
}
