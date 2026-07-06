package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.CommandStatusRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class OutboxEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventDispatcher.class);

    private static final int MAX_RETRY = 10;

    private final SpringOutboxEventRepository outboxRepository;
    private final OutboxEventSender outboxEventSender;
    private final CommandStatusRepository commandStatusRepository;

    public OutboxEventDispatcher(
            SpringOutboxEventRepository outboxRepository,
            OutboxEventSender outboxEventSender,
            CommandStatusRepository commandStatusRepository
    ) {
        this.outboxRepository = outboxRepository;
        this.outboxEventSender = outboxEventSender;
        this.commandStatusRepository = commandStatusRepository;
    }

    /**
     * Envoie les events PENDING par batch.
     * Le scheduling est porté par ScheduledOutboxEventDispatcher pour pouvoir
     * désactiver la boucle en tests tout en conservant l'appel explicite.
     */
    @Transactional
    public void dispatchPending() {
        List<OutboxEventJpaEntity> pending =
                outboxRepository.findTop50ByStatusOrderByIdAsc(OutboxStatus.PENDING);

        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventJpaEntity event : pending) {
            try {
                outboxEventSender.send(event);

                event.setStatus(OutboxStatus.SENT);
                event.setRetryCount(0);
                outboxRepository.save(event);
                commandStatusRepository.markAppliedFromEvent(event);

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
