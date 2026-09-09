package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialPublicationScheduleRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledCommandOutcomePort;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class ReconcileDispatchedArticleOperations {
    private final EditorialPublicationScheduleRepository schedules;
    private final ScheduledCommandOutcomePort outcomes;
    public ReconcileDispatchedArticleOperations(EditorialPublicationScheduleRepository schedules, ScheduledCommandOutcomePort outcomes) {
        this.schedules = schedules; this.outcomes = outcomes;
    }

    @Transactional
    public int execute(int limit) {
        int reconciled = 0;
        for (var schedule : schedules.dispatched(limit)) {
            var outcome = outcomes.find(schedule.snapshot().id());
            if (outcome.status() == ScheduledCommandOutcomePort.Outcome.Status.APPLIED) {
                schedule.complete(); schedules.save(schedule); reconciled++;
            } else if (outcome.status() == ScheduledCommandOutcomePort.Outcome.Status.REJECTED) {
                schedule.reject(outcome.reason() == null ? "Article command rejected" : outcome.reason());
                schedules.save(schedule); reconciled++;
            }
        }
        return reconciled;
    }
}
