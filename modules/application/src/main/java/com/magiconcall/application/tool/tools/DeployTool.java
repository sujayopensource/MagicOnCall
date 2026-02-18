package com.magiconcall.application.tool.tools;

import com.magiconcall.domain.tool.Tool;
import com.magiconcall.domain.tool.ToolRequest;
import com.magiconcall.domain.tool.ToolResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeployTool implements Tool {

    @Override
    public String name() { return "deploy"; }

    @Override
    public ToolResponse execute(ToolRequest request) {
        String service = request.parameters().getOrDefault("service", "unknown");
        String environment = request.parameters().getOrDefault("environment", "production");

        String content = """
            [deploy] Recent deployments for service=%s, env=%s
            #142  2024-01-15T09:15:00Z  v2.3.1 → v2.4.0  deployer=ci-bot  status=SUCCESS
            #141  2024-01-14T14:30:00Z  v2.3.0 → v2.3.1  deployer=alice   status=SUCCESS
            #140  2024-01-13T11:00:00Z  v2.2.9 → v2.3.0  deployer=ci-bot  status=ROLLED_BACK
            """.formatted(service, environment);

        return ToolResponse.success(content.strip(), Map.of(
            "service", service, "environment", environment, "deployCount", "3"
        ));
    }
}
