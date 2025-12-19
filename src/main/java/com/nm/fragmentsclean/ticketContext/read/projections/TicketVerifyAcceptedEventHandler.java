package com.nm.fragmentsclean.ticketContext.read.projections;

import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusProjectionRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TicketVerifyAcceptedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TicketVerifyAcceptedEventHandler.class);

    private final JdbcTicketStatusProjectionRepository projectionRepository;

    public TicketVerifyAcceptedEventHandler(JdbcTicketStatusProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public void handle(TicketVerifyAcceptedEvent event) {
        log.info("[ticket-read] apply TicketVerifyAcceptedEvent ticketId={} v={}",
                event.ticketId(), event.version());
        projectionRepository.applyAnalyzing(event);
    }
}
