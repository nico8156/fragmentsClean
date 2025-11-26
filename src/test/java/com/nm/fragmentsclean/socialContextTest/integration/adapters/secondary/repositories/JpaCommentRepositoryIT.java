package com.nm.fragmentsclean.socialContextTest.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.CommentJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import com.nm.fragmentsclean.socialContextTest.integration.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment.CommentSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

public class JpaCommentRepositoryIT extends AbstractJpaIntegrationTest {

    private static final UUID COMMENT_ID = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
    private static final UUID TARGET_ID  = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
    private static final UUID AUTHOR_ID  = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");

    @Autowired
    private CommentRepository commentRepository; // port DDD

    @Autowired
    private SpringCommentRepository springCommentRepository; // repo Spring Data brut

    @Test
    void can_save_a_comment() {
        var createdAt = Instant.parse("2024-01-01T10:00:00Z");
        var editedAt  = Instant.parse("2024-01-01T11:00:00Z");

        var snapshot = new CommentSnapshot(
                COMMENT_ID,
                TARGET_ID,
                AUTHOR_ID,
                null,                       // parentId
                "Hello world",              // body
                createdAt,
                editedAt,
                null,                       // deletedAt
                ModerationStatus.PUBLISHED,
                1L                          // version
        );

        commentRepository.save(Comment.fromSnapshot(snapshot));

        assertThat(springCommentRepository.findAll()).containsExactly(
                new CommentJpaEntity(
                        COMMENT_ID,
                        TARGET_ID,
                        AUTHOR_ID,
                        null,                       // parentId
                        "Hello world",
                        createdAt,
                        editedAt,
                        null,                       // deletedAt
                        ModerationStatus.PUBLISHED,
                        1L
                )
        );
    }

    @Test
    void repositories_are_injected() {
        assertThat(commentRepository).isNotNull();
        assertThat(springCommentRepository).isNotNull();
    }
    @Test
    void applyBodyEdit_changes_body_and_bumps_version_when_body_is_different() {
        var now = Instant.parse("2024-01-01T10:00:00Z");
        var comment = Comment.createNew(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "old body",
                now
        );
        var snapBefore = comment.toSnapshot();

        var editedAt = Instant.parse("2024-01-01T11:00:00Z");
        var changed = comment.applyBodyEdit("new body", editedAt);

        assertThat(changed).isTrue();
        var snapAfter = comment.toSnapshot();
        assertThat(snapAfter.body()).isEqualTo("new body");
        assertThat(snapAfter.editedAt()).isEqualTo(editedAt);
        assertThat(snapAfter.version()).isEqualTo(snapBefore.version() + 1);
    }

    @Test
    void applyBodyEdit_is_idempotent_when_same_body() {
        var now = Instant.parse("2024-01-01T10:00:00Z");
        var comment = Comment.createNew(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "same body",
                now
        );
        var snapBefore = comment.toSnapshot();

        var editedAt = Instant.parse("2024-01-01T11:00:00Z");
        var changed = comment.applyBodyEdit("same body", editedAt);

        assertThat(changed).isFalse();
        var snapAfter = comment.toSnapshot();
        assertThat(snapAfter.body()).isEqualTo("same body");
        assertThat(snapAfter.editedAt()).isNull(); // rien ne change
        assertThat(snapAfter.version()).isEqualTo(snapBefore.version());
    }
    @Test
    void can_update_a_comment() {
        var createdAt = Instant.parse("2024-01-01T10:00:00Z");
        var snapshot = new CommentSnapshot(
                COMMENT_ID,
                TARGET_ID,
                AUTHOR_ID,
                null,
                "old body",
                createdAt,
                null,
                null,
                ModerationStatus.PUBLISHED,
                0L
        );

        // on sauve une première fois
        commentRepository.save(Comment.fromSnapshot(snapshot));

        // on recharge en domaine, on applique un edit, on re-save
        var loaded = commentRepository.byId(COMMENT_ID).orElseThrow();
        var editedAt = Instant.parse("2024-01-01T11:00:00Z");
        loaded.applyBodyEdit("new body", editedAt);
        commentRepository.save(loaded);

        // on vérifie côté JPA brut
        var entity = springCommentRepository.findById(COMMENT_ID).orElseThrow();
        assertThat(entity.getBody()).isEqualTo("new body");
        assertThat(entity.getEditedAt()).isEqualTo(editedAt);
        assertThat(entity.getVersion()).isEqualTo(1L);
    }
    @Test
    void can_soft_delete_a_comment() {
        var createdAt = Instant.parse("2024-01-01T10:00:00Z");
        var snapshot = new CommentSnapshot(
                COMMENT_ID,
                TARGET_ID,
                AUTHOR_ID,
                null,
                "body",
                createdAt,
                null,
                null,
                ModerationStatus.PUBLISHED,
                0L
        );

        commentRepository.save(Comment.fromSnapshot(snapshot));

        var loaded = commentRepository.byId(COMMENT_ID).orElseThrow();
        var deletedAt = Instant.parse("2024-01-01T11:00:00Z");
        loaded.softDelete(deletedAt);
        commentRepository.save(loaded);

        var entity = springCommentRepository.findById(COMMENT_ID).orElseThrow();
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(entity.getModeration()).isEqualTo(ModerationStatus.SOFT_DELETED);
        assertThat(entity.getVersion()).isEqualTo(1L);
    }
}
