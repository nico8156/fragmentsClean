package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialPublicationScheduleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CancelArticleOperation {
    private final EditorialPublicationScheduleRepository schedules;
    public CancelArticleOperation(EditorialPublicationScheduleRepository schedules) { this.schedules = schedules; }

    @Transactional
    public void execute(UUID scheduleId) {
        var schedule = schedules.byId(scheduleId).orElseThrow(() -> new IllegalStateException("Schedule not found"));
        schedule.cancel();
        schedules.save(schedule);
    }
}
