package com.nm.fragmentsclean.socialContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository {
    Optional<Comment> byId(UUID commentId);

    void save(Comment comment);
}
