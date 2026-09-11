package com.nm.fragmentsclean.socialContext.read.projections;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcModerationProjectionRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.*;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ModerationProjectionEventHandler implements EventHandler<CommentReportedEvent> {
    private final JdbcModerationProjectionRepository repository;
    private final ProjectionSyncPublisher sync;
    public ModerationProjectionEventHandler(JdbcModerationProjectionRepository repository, ProjectionSyncPublisher sync) {
        this.repository = repository; this.sync = sync;
    }
    @Override @Transactional public void handle(CommentReportedEvent event) {
        repository.apply(event);
        sync.publish(ProjectionSyncEvent.projectionUpdated("moderation", "report", event.reportId().toString(),
                event.version(), event.occurredAt(), List.of("reported")));
    }
    @Transactional public void handle(UserBlockChangedEvent event) {
        repository.apply(event);
        sync.publish(ProjectionSyncEvent.projectionUpdated("blocked-users", "user", event.blockerId().toString(),
                event.version(), event.occurredAt(), List.of(event.active() ? "blocked" : "unblocked")));
    }
    @Transactional public void handle(CommentModeratedEvent event) {
        repository.apply(event);
        sync.publish(ProjectionSyncEvent.projectionUpdated("comments", "target", event.targetId().toString(),
                event.version(), event.occurredAt(), List.of("moderated")));
        sync.publish(ProjectionSyncEvent.projectionUpdated("moderation", "report", event.reportId().toString(),
                event.version(), event.occurredAt(), List.of("decided")));
    }
}
