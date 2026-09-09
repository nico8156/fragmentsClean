package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.primary.springboot.scheduling;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.DispatchDueArticleOperations;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ReconcileDispatchedArticleOperations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "fragments.editorial.planning.schedule.enabled", havingValue = "true")
public final class EditorialPublicationScheduleJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditorialPublicationScheduleJob.class);
    private final DispatchDueArticleOperations dispatch;
    private final ReconcileDispatchedArticleOperations reconcile;
    private final String worker = "editorial-planning-" + UUID.randomUUID();

    public EditorialPublicationScheduleJob(DispatchDueArticleOperations dispatch,
                                           ReconcileDispatchedArticleOperations reconcile) {
        this.dispatch = dispatch; this.reconcile = reconcile;
    }

    @Scheduled(fixedDelayString = "${fragments.editorial.planning.schedule.delay-ms:60000}")
    public void tick() {
        try {
            reconcile.execute(50);
        } catch (RuntimeException failure) {
            LOGGER.error("editorial_schedule_reconciliation_failed worker={}", worker, failure);
        }
        try {
            dispatch.execute(20, Duration.ofMinutes(5), worker);
        } catch (RuntimeException failure) {
            LOGGER.error("editorial_schedule_dispatch_failed worker={}", worker, failure);
        }
    }
}
