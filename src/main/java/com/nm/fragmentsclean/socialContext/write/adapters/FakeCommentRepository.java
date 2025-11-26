package com.nm.fragmentsclean.socialContext.write.adapters;

import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;

import java.util.*;

public class FakeCommentRepository implements CommentRepository {
    private final Map<UUID, Comment.CommentSnapshot> store = new LinkedHashMap<>();

    @Override
    public Optional<Comment> byId(UUID commentId) {
        return Optional.ofNullable(store.get(commentId))
                .map(Comment::fromSnapshot);
    }

    @Override
    public void save(Comment comment) {
        store.put(comment.id(), comment.toSnapshot());
    }

    // utilitaire de test (comme allSnapshots() côté Like)
    public List<Comment.CommentSnapshot> allSnapshots() {
        return new ArrayList<>(store.values());
    }
}
