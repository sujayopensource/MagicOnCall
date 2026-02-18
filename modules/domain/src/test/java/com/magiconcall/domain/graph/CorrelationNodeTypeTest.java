package com.magiconcall.domain.graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationNodeTypeTest {

    @Test
    @DisplayName("CorrelationNodeType has all 6 expected values")
    void hasAllValues() {
        assertThat(CorrelationNodeType.values()).containsExactlyInAnyOrder(
            CorrelationNodeType.ALERT,
            CorrelationNodeType.METRIC_ANOMALY,
            CorrelationNodeType.LOG_CLUSTER,
            CorrelationNodeType.DEPLOY,
            CorrelationNodeType.SERVICE,
            CorrelationNodeType.DEPENDENCY
        );
    }

    @Test
    @DisplayName("valueOf roundtrips correctly")
    void valueOfRoundtrip() {
        for (var type : CorrelationNodeType.values()) {
            assertThat(CorrelationNodeType.valueOf(type.name())).isEqualTo(type);
        }
    }
}
