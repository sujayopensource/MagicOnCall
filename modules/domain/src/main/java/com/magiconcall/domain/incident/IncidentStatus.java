package com.magiconcall.domain.incident;

import java.util.Map;
import java.util.Set;

/**
 * Incident lifecycle state machine.
 *
 * NEW → TRIAGING → INVESTIGATING → MITIGATING → MONITORING → RESOLVED → POSTMORTEM
 *
 * Force-close: TRIAGING/INVESTIGATING/MITIGATING/MONITORING → RESOLVED
 */
public enum IncidentStatus {
    NEW,
    TRIAGING,
    INVESTIGATING,
    MITIGATING,
    MONITORING,
    RESOLVED,
    POSTMORTEM;

    private static final Map<IncidentStatus, Set<IncidentStatus>> VALID_TRANSITIONS = Map.of(
        NEW,           Set.of(TRIAGING),
        TRIAGING,      Set.of(INVESTIGATING, RESOLVED),
        INVESTIGATING, Set.of(MITIGATING, RESOLVED),
        MITIGATING,    Set.of(MONITORING, RESOLVED),
        MONITORING,    Set.of(RESOLVED),
        RESOLVED,      Set.of(POSTMORTEM),
        POSTMORTEM,    Set.of()
    );

    public boolean canTransitionTo(IncidentStatus target) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public void validateTransitionTo(IncidentStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidTransitionException(this, target);
        }
    }

    public static class InvalidTransitionException extends RuntimeException {
        private final IncidentStatus from;
        private final IncidentStatus to;

        public InvalidTransitionException(IncidentStatus from, IncidentStatus to) {
            super("Invalid incident transition: %s → %s".formatted(from, to));
            this.from = from;
            this.to = to;
        }

        public IncidentStatus getFrom() { return from; }
        public IncidentStatus getTo() { return to; }
    }
}
