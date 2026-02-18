package com.magiconcall.api.graph;

import com.magiconcall.application.graph.BlastRadiusResult;

import java.util.List;

public record BlastRadiusResponse(
    GraphNodeResponse rootCauseNode,
    List<GraphNodeResponse> affectedNodes,
    int totalAffected
) {
    public static BlastRadiusResponse from(BlastRadiusResult result) {
        return new BlastRadiusResponse(
            GraphNodeResponse.from(result.rootCauseNode()),
            result.affectedNodes().stream().map(GraphNodeResponse::from).toList(),
            result.totalAffected()
        );
    }
}
