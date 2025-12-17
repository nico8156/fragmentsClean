package com.nm.fragmentsclean.socialContext.read.projections;


import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcCommentProjectionRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CommentUpdatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CommentUpdatedEventHandler.class);

    private final JdbcCommentProjectionRepository projectionRepository;

    public CommentUpdatedEventHandler(JdbcCommentProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public void handle(CommentUpdatedEvent event) {
        log.info("[social-read] apply CommentUpdatedEvent commentId={} v={}", event.commentId(), event.version());
        projectionRepository.apply(event);
    }
}
