package com.magiconcall.api.graph;

import com.magiconcall.application.graph.CorrelationGraphResult;

import java.util.List;

public record CorrelationGraphResponse(
    List<GraphNodeResponse> nodes,
    List<GraphEdgeResponse> edges
) {
    public static CorrelationGraphResponse from(CorrelationGraphResult result) {
        return new CorrelationGraphResponse(
            result.nodes().stream().map(GraphNodeResponse::from).toList(),
            result.edges().stream().map(GraphEdgeResponse::from).toList()
        );
    }
}
