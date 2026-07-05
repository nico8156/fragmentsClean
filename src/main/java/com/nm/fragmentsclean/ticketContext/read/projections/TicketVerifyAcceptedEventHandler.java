package com.nm.fragmentsclean.ticketContext.read.projections;

import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusProjectionRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TicketVerifyAcceptedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TicketVerifyAcceptedEventHandler.class);

    private final JdbcTicketStatusProjectionRepository projectionRepository;
    private final ProjectionSyncPublisher projectionSyncPublisher;

    public TicketVerifyAcceptedEventHandler(
            JdbcTicketStatusProjectionRepository projectionRepository,
            ProjectionSyncPublisher projectionSyncPublisher) {
        this.projectionRepository = projectionRepository;
        this.projectionSyncPublisher = projectionSyncPublisher;
    }

    public void handle(TicketVerifyAcceptedEvent event) {
        log.info("[ticket-read] apply TicketVerifyAcceptedEvent ticketId={} v={}",
                event.ticketId(), event.version());
        projectionRepository.applyAnalyzing(event);
        projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
                "tickets",
                "entity",
                event.ticketId().toString(),
                event.version(),
                event.occurredAt(),
                List.of("status", "analyzing")));
    }
}
