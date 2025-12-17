package com.nm.fragmentsclean.socialContext.read.projections;


import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcCommentProjectionRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CommentDeletedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CommentDeletedEventHandler.class);

    private final JdbcCommentProjectionRepository projectionRepository;

    public CommentDeletedEventHandler(JdbcCommentProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public void handle(CommentDeletedEvent event) {
        log.info("[social-read] apply CommentDeletedEvent commentId={} v={}", event.commentId(), event.version());
        projectionRepository.apply(event);
    }
}
