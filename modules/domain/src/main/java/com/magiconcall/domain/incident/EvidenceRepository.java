package com.magiconcall.domain.incident;

import java.util.List;
import java.util.UUID;

public interface EvidenceRepository {

    Evidence save(Evidence evidence);

    List<Evidence> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
