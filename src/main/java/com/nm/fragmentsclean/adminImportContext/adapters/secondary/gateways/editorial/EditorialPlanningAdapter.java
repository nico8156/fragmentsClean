package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialPlanningPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.CancelArticleOperation;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ScheduleArticleOperation;

import java.time.Instant;
import java.util.UUID;

public final class EditorialPlanningAdapter implements EditorialPlanningPort {
    private final ScheduleArticleOperation schedule;
    private final CancelArticleOperation cancel;
    public EditorialPlanningAdapter(ScheduleArticleOperation schedule, CancelArticleOperation cancel) {
        this.schedule = schedule; this.cancel = cancel;
    }
    @Override public UUID schedule(UUID scheduleId, UUID articleId, UUID revisionId, String operation, Instant dueAt) {
        return schedule.execute(scheduleId, articleId, revisionId, operation, dueAt);
    }
    @Override public void cancel(UUID scheduleId) { cancel.execute(scheduleId); }
}
