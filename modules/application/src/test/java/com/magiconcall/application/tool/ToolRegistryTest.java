package com.magiconcall.application.tool;

import com.magiconcall.domain.tool.Tool;
import com.magiconcall.domain.tool.ToolRequest;
import com.magiconcall.domain.tool.ToolResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRegistryTest {

    private final Tool testTool = new Tool() {
        @Override public String name() { return "test-tool"; }
        @Override public ToolResponse execute(ToolRequest request) {
            return ToolResponse.success("ok", Map.of());
        }
    };

    private final ToolRegistry registry = new ToolRegistry(List.of(testTool));

    @Test
    @DisplayName("finds tool by name")
    void findByName() {
        var found = registry.findByName("test-tool");
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("test-tool");
    }

    @Test
    @DisplayName("throws ToolNotFoundException for unknown tool")
    void throwsForUnknownTool() {
        assertThatThrownBy(() -> registry.getByName("nonexistent"))
            .isInstanceOf(ToolRegistry.ToolNotFoundException.class)
            .hasMessageContaining("nonexistent");
    }

    @Test
    @DisplayName("lists all available tool names")
    void listsAllTools() {
        var tools = registry.availableTools();
        assertThat(tools).containsExactly("test-tool");
    }
}
