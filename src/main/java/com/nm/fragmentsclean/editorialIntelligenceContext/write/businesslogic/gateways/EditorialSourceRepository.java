package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EditorialSourceRepository {
    Optional<EditorialSource> byId(UUID sourceId);
    List<EditorialSource> dueAt(Instant now, int limit);
    void save(EditorialSource source);
}
