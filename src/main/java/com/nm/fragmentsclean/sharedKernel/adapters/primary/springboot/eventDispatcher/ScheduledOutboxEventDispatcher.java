package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.outbox.dispatcher.scheduling-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ScheduledOutboxEventDispatcher {

    private final OutboxEventDispatcher dispatcher;

    public ScheduledOutboxEventDispatcher(OutboxEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${app.outbox.dispatcher.delay-ms:500}")
    public void dispatchPending() {
        dispatcher.dispatchPending();
    }
}
