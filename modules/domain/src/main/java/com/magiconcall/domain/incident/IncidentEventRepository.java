package com.magiconcall.domain.incident;

import java.util.List;
import java.util.UUID;

public interface IncidentEventRepository {

    IncidentEvent save(IncidentEvent event);

    List<IncidentEvent> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
