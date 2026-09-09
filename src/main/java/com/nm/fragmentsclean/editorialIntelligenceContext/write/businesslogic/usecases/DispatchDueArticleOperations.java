package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialPublicationScheduleRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledArticleOperationPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledCommandOutcomePort;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

@Component
public final class DispatchDueArticleOperations {
    private final EditorialPublicationScheduleRepository schedules;
    private final ScheduledArticleOperationPort operations;
    private final ScheduledCommandOutcomePort outcomes;
    private final DateTimeProvider clock;
    private final TransactionTemplate transactions;

    public DispatchDueArticleOperations(EditorialPublicationScheduleRepository schedules,
                                        ScheduledArticleOperationPort operations,
                                        ScheduledCommandOutcomePort outcomes,
                                        DateTimeProvider clock, TransactionTemplate transactions) {
        this.schedules = schedules; this.operations = operations; this.outcomes = outcomes;
        this.clock = clock; this.transactions = transactions;
    }

    public int execute(int limit, Duration leaseDuration, String worker) {
        int dispatched = 0;
        for (var candidate : schedules.claimableAt(clock.now(), limit)) {
            var claimed = transactions.execute(status -> {
                var current = schedules.byId(candidate.snapshot().id()).orElseThrow();
                current.claim(worker, clock.now(), clock.now().plus(leaseDuration));
                schedules.save(current);
                return current.snapshot();
            });
            if (claimed == null) continue;
            try {
                if (claimed.operation() == com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialPublicationSchedule.Operation.PUBLISH) {
                    operations.publish(claimed.id(), clock.now(), claimed.articleId(), claimed.revisionId());
                } else {
                    operations.archive(claimed.id(), clock.now(), claimed.articleId());
                }
            } catch (RuntimeException failure) {
                // A synchronous command handler may persist APPLIED/REJECTED before propagating.
                // Only acknowledge dispatch when the canonical command store proves acceptance.
                if (outcomes.find(claimed.id()).status() == ScheduledCommandOutcomePort.Outcome.Status.PENDING) {
                    continue;
                }
            }
            transactions.executeWithoutResult(status -> {
                var current = schedules.byId(claimed.id()).orElseThrow();
                current.markDispatched(worker, clock.now());
                schedules.save(current);
            });
            dispatched++;
        }
        return dispatched;
    }
}
