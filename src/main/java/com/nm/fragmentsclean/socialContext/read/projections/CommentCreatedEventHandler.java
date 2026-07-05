package com.nm.fragmentsclean.socialContext.read.projections;

import java.util.List;

import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcCommentProjectionRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CommentCreatedEventHandler implements EventHandler<CommentCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(CommentCreatedEventHandler.class);

    private final JdbcCommentProjectionRepository projectionRepository;
    private final ProjectionSyncPublisher projectionSyncPublisher;

    public CommentCreatedEventHandler(
            JdbcCommentProjectionRepository projectionRepository,
            ProjectionSyncPublisher projectionSyncPublisher) {
        this.projectionRepository = projectionRepository;
        this.projectionSyncPublisher = projectionSyncPublisher;
    }

    @Override
    @Transactional
    public void handle(CommentCreatedEvent event) {
        log.info("[social-read] apply CommentCreatedEvent commentId={} v={}", event.commentId(), event.version());
        projectionRepository.apply(event);
        projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
                "comments",
                "target",
                event.targetId().toString(),
                event.version(),
                event.occurredAt(),
                List.of("created")));
    }
}
