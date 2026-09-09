package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialPublicationSchedule;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EditorialPublicationScheduleRepository {
    void save(EditorialPublicationSchedule schedule) throws EditorialScheduleConcurrencyException;
    Optional<EditorialPublicationSchedule> byId(UUID scheduleId);
    List<EditorialPublicationSchedule> claimableAt(Instant now, int limit);
    List<EditorialPublicationSchedule> dispatched(int limit);
}
