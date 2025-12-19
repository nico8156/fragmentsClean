package com.nm.fragmentsclean.ticketContext.read.projections;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusProjectionRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TicketVerificationCompletedEventHandler implements EventHandler<TicketVerificationCompletedEvent> {

    private static final Logger log = LoggerFactory.getLogger(TicketVerificationCompletedEventHandler.class);

    private final JdbcTicketStatusProjectionRepository projectionRepository;

    public TicketVerificationCompletedEventHandler(JdbcTicketStatusProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public void handle(TicketVerificationCompletedEvent event) {
        log.info("[ticket-read] apply TicketVerificationCompletedEvent ticketId={} outcome={} v={}",
                event.ticketId(), event.outcome(), event.version());
        projectionRepository.applyCompleted(event);
    }
}
