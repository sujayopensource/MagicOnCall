package com.magiconcall.domain.incident;

import java.util.List;
import java.util.UUID;

public interface HypothesisRepository {

    Hypothesis save(Hypothesis hypothesis);

    List<Hypothesis> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
