package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventDispatcher.class);

    private static final int MAX_RETRY = 10;

    private final SpringOutboxEventRepository outboxRepository;
    private final OutboxEventSender outboxEventSender; // CompositeOutboxEventSender en pratique

    public OutboxEventDispatcher(
            SpringOutboxEventRepository outboxRepository,
            OutboxEventSender outboxEventSender
    ) {
        this.outboxRepository = outboxRepository;
        this.outboxEventSender = outboxEventSender;
    }

    /**
     * Tâche périodique : envoie les events PENDING par batch.
     * Tu peux ajuster fixedDelay via properties :
     *   app.outbox.dispatcher.delay-ms=500
     */
    @Scheduled(fixedDelayString = "${app.outbox.dispatcher.delay-ms:500}")
    @Transactional
    public void dispatchPending() {
        List<OutboxEventJpaEntity> pending =
                outboxRepository.findTop50ByStatusOrderByIdAsc(OutboxStatus.PENDING);

        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventJpaEntity event : pending) {
            try {
                // Délégation à l'adapter (CompositeOutboxEventSender)
                outboxEventSender.send(event);

                event.setStatus(OutboxStatus.SENT);
                event.setRetryCount(0);
                outboxRepository.save(event);

            } catch (Exception e) {
                log.error("Failed to send outbox event id={} type={}",
                        event.getId(), event.getEventType(), e);
                handleFailure(event);
            }
        }
    }

    private void handleFailure(OutboxEventJpaEntity event) {
        int currentRetry = event.getRetryCount() != null ? event.getRetryCount() : 0;
        currentRetry++;

        event.setRetryCount(currentRetry);

        if (currentRetry >= MAX_RETRY) {
            event.setStatus(OutboxStatus.FAILED);
        } else {
            event.setStatus(OutboxStatus.PENDING);
        }

        outboxRepository.save(event);
    }
}
