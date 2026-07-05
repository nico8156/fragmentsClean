package com.nm.fragmentsclean.ticketContext.read.projections;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusProjectionRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TicketVerificationCompletedEventHandler implements EventHandler<TicketVerificationCompletedEvent> {

    private static final Logger log = LoggerFactory.getLogger(TicketVerificationCompletedEventHandler.class);

    private final JdbcTicketStatusProjectionRepository projectionRepository;
    private final ProjectionSyncPublisher projectionSyncPublisher;

    public TicketVerificationCompletedEventHandler(
            JdbcTicketStatusProjectionRepository projectionRepository,
            ProjectionSyncPublisher projectionSyncPublisher) {
        this.projectionRepository = projectionRepository;
        this.projectionSyncPublisher = projectionSyncPublisher;
    }

    public void handle(TicketVerificationCompletedEvent event) {
        log.info("[ticket-read] apply TicketVerificationCompletedEvent ticketId={} outcome={} v={}",
                event.ticketId(), event.outcome(), event.version());
        projectionRepository.applyCompleted(event);
        projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
                "tickets",
                "entity",
                event.ticketId().toString(),
                event.version(),
                event.occurredAt(),
                List.of("status", event.outcome().name().toLowerCase())));
    }
}
