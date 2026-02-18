package com.magiconcall.domain.graph;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationEdgeTypeTest {

    @Test
    @DisplayName("CorrelationEdgeType has all 4 expected values")
    void hasAllValues() {
        assertThat(CorrelationEdgeType.values()).containsExactlyInAnyOrder(
            CorrelationEdgeType.TIME_CORRELATION,
            CorrelationEdgeType.DEPENDS_ON,
            CorrelationEdgeType.CAUSAL_HINT,
            CorrelationEdgeType.SAME_RELEASE
        );
    }

    @Test
    @DisplayName("valueOf roundtrips correctly")
    void valueOfRoundtrip() {
        for (var type : CorrelationEdgeType.values()) {
            assertThat(CorrelationEdgeType.valueOf(type.name())).isEqualTo(type);
        }
    }
}
