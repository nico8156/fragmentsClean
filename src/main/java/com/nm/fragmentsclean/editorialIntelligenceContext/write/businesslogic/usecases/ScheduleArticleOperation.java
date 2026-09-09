package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialPublicationScheduleRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialPublicationSchedule;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ScheduleArticleOperation {
    private final EditorialPublicationScheduleRepository schedules;
    private final DateTimeProvider clock;

    public ScheduleArticleOperation(EditorialPublicationScheduleRepository schedules, DateTimeProvider clock) {
        this.schedules = schedules;
        this.clock = clock;
    }

    @Transactional
    public UUID execute(UUID scheduleId, UUID articleId, UUID revisionId, String operation, Instant dueAt) {
        var schedule = EditorialPublicationSchedule.schedule(scheduleId, articleId, revisionId,
                EditorialPublicationSchedule.Operation.valueOf(operation), dueAt, clock.now());
        schedules.save(schedule);
        return scheduleId;
    }
}
