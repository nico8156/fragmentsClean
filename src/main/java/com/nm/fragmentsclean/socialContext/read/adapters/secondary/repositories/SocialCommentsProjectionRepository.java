package com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;

public interface SocialCommentsProjectionRepository {

    void apply(CommentCreatedEvent event);

    void apply(CommentUpdatedEvent event);

    void apply(CommentDeletedEvent event);
}
