package com.nm.fragmentsclean.socialContext.read.projections;

import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcCommentProjectionRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CommentCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CommentCreatedEventHandler.class);

    private final JdbcCommentProjectionRepository projectionRepository;

    public CommentCreatedEventHandler(JdbcCommentProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public void handle(CommentCreatedEvent event) {
        log.info("[social-read] apply CommentCreatedEvent commentId={} v={}", event.commentId(), event.version());
        projectionRepository.apply(event);
    }
}
