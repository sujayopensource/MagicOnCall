package com.magiconcall.application.graph;

import java.util.List;

public record BlastRadiusResult(
    GraphNodeResult rootCauseNode,
    List<GraphNodeResult> affectedNodes,
    int totalAffected
) {}
