package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxMessage;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxRetryPolicy;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxDeliveryStore;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OutboxEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventDispatcher.class);

    private final OutboxDeliveryStore deliveryStore;
    private final OutboxEventSender sender;
    private final DateTimeProvider clock;
    private final OutboxRetryPolicy retryPolicy;
    private final Duration leaseDuration;
    private final int batchSize;
    private final String workerId;

    public OutboxEventDispatcher(
            OutboxDeliveryStore deliveryStore,
            OutboxEventSender sender,
            DateTimeProvider clock,
            OutboxRetryPolicy retryPolicy,
            Duration leaseDuration,
            int batchSize,
            String workerId
    ) {
        if (leaseDuration.isNegative() || leaseDuration.isZero() || batchSize < 1) {
            throw new IllegalArgumentException("Invalid outbox dispatcher configuration");
        }
        this.deliveryStore = deliveryStore;
        this.sender = sender;
        this.clock = clock;
        this.retryPolicy = retryPolicy;
        this.leaseDuration = leaseDuration;
        this.batchSize = batchSize;
        this.workerId = workerId;
    }

    /**
     * Envoie les events PENDING par batch.
     * Le scheduling est porté par ScheduledOutboxEventDispatcher pour pouvoir
     * désactiver la boucle en tests tout en conservant l'appel explicite.
     */
    public void dispatchPending() {
        String owner = workerId + ':' + UUID.randomUUID();
        int remaining = batchSize;
        while (remaining > 0) {
            Instant claimedAt = clock.now();
            var claimed = deliveryStore.claimDue(owner, claimedAt, claimedAt.plus(leaseDuration), remaining);
            if (claimed.isEmpty()) return;
            for (OutboxMessage message : claimed) deliver(message, owner);
            remaining -= claimed.size();
        }
    }

    private void deliver(OutboxMessage message, String owner) {
        try {
            sender.send(message);
            if (!deliveryStore.markSent(message, owner, clock.now())) {
                log.warn("Outbox completion lost its lease eventId={} owner={}", message.eventId(), owner);
            }
        } catch (Exception failure) {
            int failureCount = message.retryCount() + 1;
            boolean terminal = retryPolicy.terminal(failureCount);
            Instant nextAttemptAt = clock.now().plus(retryPolicy.delayFor(message.eventId(), failureCount));
            boolean recorded;
            try {
                recorded = deliveryStore.recordFailure(message.id(), owner, failureCount, nextAttemptAt,
                        failure.getClass().getSimpleName() + ": " + failure.getMessage(), terminal);
            } catch (RuntimeException persistenceFailure) {
                log.error("Outbox delivery and failure persistence both failed eventId={} owner={}",
                        message.eventId(), owner, persistenceFailure);
                return;
            }
            if (!recorded) {
                log.warn("Outbox failure lost its lease eventId={} owner={}", message.eventId(), owner);
            } else if (terminal) {
                log.error("Outbox event exhausted retries eventId={} type={} failures={}",
                        message.eventId(), message.eventType(), failureCount, failure);
            } else {
                log.warn("Outbox delivery failed; retry scheduled eventId={} type={} failures={} nextAttemptAt={}",
                        message.eventId(), message.eventType(), failureCount, nextAttemptAt, failure);
            }
        }
    }
}
