package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.CommentJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WriteCommentUpdateControllerIT extends AbstractBaseE2E {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COMMENT_ID = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
    private static final UUID USER_ID    = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
    private static final UUID TARGET_ID  = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringCommentRepository springCommentRepository;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @BeforeEach
    void setup() {
        springCommentRepository.deleteAll();

        // instant "serveur" utilisé par le domaine (DateTimeProvider)
        ((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow =
                Instant.parse("2024-01-01T10:00:00Z");
    }

    @Test
    void can_update_comment_body() throws Exception {
        // given : un commentaire déjà présent en base
        var createdAt = Instant.parse("2024-01-01T08:00:00Z");

        springCommentRepository.save(
                new CommentJpaEntity(
                        COMMENT_ID,
                        TARGET_ID,
                        USER_ID,
                        null,                       // parentId
                        "Old body",
                        createdAt,
                        null,                       // editedAt
                        null,                       // deletedAt
                        ModerationStatus.PUBLISHED,
                        0L                          // version initiale
                )
        );

        var clientEditedAt = "2024-01-01T09:00:00Z";

        // when : appel du PUT /api/social/comments
        mockMvc.perform(
                        put("/api/social/comments")
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "commandId": "%s",
                                          "commentId": "%s",
                                          "body": "New body",
                                          "editedAt": "%s"
                                        }
                                        """.formatted(
                                                COMMAND_ID,
                                                COMMENT_ID,
                                                clientEditedAt
                                        )
                                )
                )
                .andExpect(status().isAccepted());

        // then : vérif côté JPA brut
        var entities = springCommentRepository.findAll();
        assertThat(entities).hasSize(1);

        var entity = entities.get(0);
        assertThat(entity.getCommentId()).isEqualTo(COMMENT_ID);
        assertThat(entity.getTargetId()).isEqualTo(TARGET_ID);
        assertThat(entity.getAuthorId()).isEqualTo(USER_ID);
        assertThat(entity.getBody()).isEqualTo("New body");
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        var now = Instant.parse("2024-01-01T10:00:00Z"); // le "now" du serveur, fixé par DeterministicDateTimeProvider
        assertThat(entity.getEditedAt()).isEqualTo(now);

        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.getModeration()).isEqualTo(ModerationStatus.PUBLISHED);
        assertThat(entity.getVersion()).isEqualTo(1L); // version ++ après update
    }
}
