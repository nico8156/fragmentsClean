package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.CommentJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;

import java.util.Optional;
import java.util.UUID;



public class JpaCommentRepository implements CommentRepository {

    private final SpringCommentRepository springCommentRepository;

    public JpaCommentRepository(SpringCommentRepository springCommentRepository) {
        this.springCommentRepository = springCommentRepository;
    }

    @Override
    public Optional<Comment> byId(UUID commentId) {
        return springCommentRepository.findById(commentId)
                .map(this::toDomain);
    }

    @Override
    public void save(Comment comment) {
        springCommentRepository.save(toJpa(comment));
    }

    // ----- mapping -----

    private Comment toDomain(CommentJpaEntity entity) {
        return Comment.fromSnapshot(
                new Comment.CommentSnapshot(
                        entity.getCommentId(),
                        entity.getTargetId(),
                        entity.getAuthorId(),
                        entity.getParentId(),
                        entity.getBody(),
                        entity.getCreatedAt(),
                        entity.getEditedAt(),
                        entity.getDeletedAt(),
                        entity.getModeration(),
                        entity.getVersion()
                )
        );
    }

    private CommentJpaEntity toJpa(Comment comment) {
        var snap = comment.toSnapshot();
        return new CommentJpaEntity(
                snap.commentId(),
                snap.targetId(),
                snap.authorId(),
                snap.parentId(),
                snap.body(),
                snap.createdAt(),
                snap.editedAt(),
                snap.deletedAt(),
                snap.moderation(),
                snap.version()
        );
    }
}
