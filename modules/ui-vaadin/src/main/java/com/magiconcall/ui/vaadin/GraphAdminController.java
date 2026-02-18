package com.magiconcall.ui.vaadin;

import com.magiconcall.application.graph.CorrelationGraphService;
import com.magiconcall.domain.tenant.TenantContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/graph")
public class GraphAdminController {

    private final CorrelationGraphService graphService;

    public GraphAdminController(CorrelationGraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping(value = "/{incidentId}", produces = MediaType.TEXT_HTML_VALUE)
    public String graphView(@PathVariable UUID incidentId,
                            @RequestParam(defaultValue = "admin") String tenantId) {
        TenantContext.setTenantId(tenantId);
        try {
            var graph = graphService.getGraph(incidentId);
            var sb = new StringBuilder();
            sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'>");
            sb.append("<title>Graph Admin — ").append(incidentId).append("</title>");
            sb.append("<style>");
            sb.append("body{font-family:system-ui,sans-serif;margin:24px;color:#1f2937}");
            sb.append("table{border-collapse:collapse;width:100%;margin-bottom:24px}");
            sb.append("th,td{border:1px solid #d1d5db;padding:8px 12px;text-align:left;font-size:13px}");
            sb.append("th{background:#f3f4f6;font-weight:600}");
            sb.append("h1{font-size:20px}h2{font-size:16px;margin-top:24px}");
            sb.append(".badge{display:inline-block;padding:2px 8px;border-radius:4px;font-size:11px;font-weight:600}");
            sb.append("</style></head><body>");
            sb.append("<h1>Correlation Graph — ").append(incidentId).append("</h1>");

            // Nodes table
            sb.append("<h2>Nodes (").append(graph.nodes().size()).append(")</h2>");
            sb.append("<table><tr><th>ID</th><th>Type</th><th>Label</th><th>Description</th><th>Source</th><th>Created</th></tr>");
            for (var node : graph.nodes()) {
                sb.append("<tr>");
                sb.append("<td>").append(node.id().toString(), 0, 8).append("...</td>");
                sb.append("<td><span class='badge'>").append(node.nodeType()).append("</span></td>");
                sb.append("<td>").append(escapeHtml(node.label())).append("</td>");
                sb.append("<td>").append(node.description() != null ? escapeHtml(node.description()) : "—").append("</td>");
                sb.append("<td>").append(node.source() != null ? escapeHtml(node.source()) : "—").append("</td>");
                sb.append("<td>").append(node.createdAt()).append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");

            // Edges table
            sb.append("<h2>Edges (").append(graph.edges().size()).append(")</h2>");
            sb.append("<table><tr><th>ID</th><th>Type</th><th>Source Node</th><th>Target Node</th><th>Weight</th><th>Reason</th></tr>");
            for (var edge : graph.edges()) {
                sb.append("<tr>");
                sb.append("<td>").append(edge.id().toString(), 0, 8).append("...</td>");
                sb.append("<td><span class='badge'>").append(edge.edgeType()).append("</span></td>");
                sb.append("<td>").append(edge.sourceNodeId().toString(), 0, 8).append("...</td>");
                sb.append("<td>").append(edge.targetNodeId().toString(), 0, 8).append("...</td>");
                sb.append("<td>").append(String.format("%.2f", edge.weight())).append("</td>");
                sb.append("<td>").append(edge.reason() != null ? escapeHtml(edge.reason()) : "—").append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");

            sb.append("</body></html>");
            return sb.toString();
        } finally {
            TenantContext.clear();
        }
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
