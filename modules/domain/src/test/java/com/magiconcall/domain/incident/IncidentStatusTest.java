package com.magiconcall.domain.incident;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentStatusTest {

    @ParameterizedTest
    @DisplayName("valid transitions succeed")
    @CsvSource({
        "NEW, TRIAGING",
        "TRIAGING, INVESTIGATING",
        "TRIAGING, RESOLVED",
        "INVESTIGATING, MITIGATING",
        "INVESTIGATING, RESOLVED",
        "MITIGATING, MONITORING",
        "MITIGATING, RESOLVED",
        "MONITORING, RESOLVED",
        "RESOLVED, POSTMORTEM"
    })
    void validTransitions(IncidentStatus from, IncidentStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("invalid transitions are rejected")
    @CsvSource({
        "NEW, INVESTIGATING",
        "NEW, RESOLVED",
        "TRIAGING, MITIGATING",
        "INVESTIGATING, TRIAGING",
        "MONITORING, INVESTIGATING",
        "RESOLVED, NEW",
        "POSTMORTEM, NEW",
        "POSTMORTEM, RESOLVED"
    })
    void invalidTransitions(IncidentStatus from, IncidentStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    @DisplayName("validateTransitionTo throws on invalid transition")
    void validateTransitionThrows() {
        assertThatThrownBy(() -> IncidentStatus.NEW.validateTransitionTo(IncidentStatus.RESOLVED))
            .isInstanceOf(IncidentStatus.InvalidTransitionException.class)
            .hasMessageContaining("NEW")
            .hasMessageContaining("RESOLVED");
    }

    @Test
    @DisplayName("Incident entity enforces state machine")
    void incidentTransitionEnforced() {
        var incident = new Incident("ext-1", "Test", "Summary",
            IncidentSeverity.SEV2, "commander", "{}");

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.NEW);

        IncidentStatus prev = incident.transitionTo(IncidentStatus.TRIAGING);
        assertThat(prev).isEqualTo(IncidentStatus.NEW);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.TRIAGING);

        incident.transitionTo(IncidentStatus.INVESTIGATING);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);

        incident.transitionTo(IncidentStatus.MITIGATING);
        incident.transitionTo(IncidentStatus.MONITORING);
        incident.transitionTo(IncidentStatus.RESOLVED);
        incident.transitionTo(IncidentStatus.POSTMORTEM);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.POSTMORTEM);
    }

    @Test
    @DisplayName("force-close from INVESTIGATING to RESOLVED is valid")
    void forceCloseFromInvestigating() {
        var incident = new Incident("ext-2", "Test", null,
            IncidentSeverity.SEV1, null, "{}");
        incident.transitionTo(IncidentStatus.TRIAGING);
        incident.transitionTo(IncidentStatus.INVESTIGATING);
        incident.transitionTo(IncidentStatus.RESOLVED);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
    }
}
