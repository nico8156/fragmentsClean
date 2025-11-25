package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.crons;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventDispatcher {

    private final SpringOutboxEventRepository outboxRepo;
    private final OutboxEventSender eventSender;

    public OutboxEventDispatcher(SpringOutboxEventRepository outboxRepo,
                                 OutboxEventSender eventSender) {
        this.outboxRepo = outboxRepo;
        this.eventSender = eventSender;
    }

    @Scheduled(fixedDelay = 1000) // toutes les 1s (à ajuster)
    @Transactional
    public void dispatch() {
        var pendingEvents = outboxRepo.findTop100ByStatusOrderByIdAsc(OutboxStatus.PENDING);

        for (var outboxEvent : pendingEvents) {
            try {
                eventSender.send(outboxEvent);

                outboxEvent.setStatus(OutboxStatus.SENT);
                outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);

            } catch (Exception e) {
                // Politique simple : FAIL + retryCount++
                outboxEvent.setStatus(OutboxStatus.FAILED);
                outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
                // logger l'erreur, etc.
            }
        }
    }
}
