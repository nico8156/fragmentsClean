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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WriteCommentControllerIT extends AbstractBaseE2E {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COMMENT_ID = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
    private static final UUID USER_ID    = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
    private static final UUID TARGET_ID  = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");

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
    void can_create_comment() throws Exception {
        var clientAt = "2024-01-01T09:00:00Z";

        mockMvc.perform(
                        post("/api/social/comments") // <-- mapping du WriteCommentController
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "commandId": "%s",
                                          "commentId": "%s",
                                          "userId": "%s",
                                          "targetId": "%s",
                                          "parentId": null,
                                          "body": "Hello world",
                                          "at": "%s"
                                        }
                                        """.formatted(
                                                COMMAND_ID,
                                                COMMENT_ID,
                                                USER_ID,
                                                TARGET_ID,
                                                clientAt
                                        )
                                )
                )
                .andExpect(status().isAccepted()); // 202 comme pour les likes

        var now = Instant.parse("2024-01-01T10:00:00Z"); // date "serveur"

        assertThat(springCommentRepository.findAll()).containsExactly(
                new CommentJpaEntity(
                        COMMENT_ID,
                        TARGET_ID,
                        USER_ID,
                        null,                       // parentId
                        "Hello world",
                        now,                        // createdAt (serveur)
                        null,                       // editedAt
                        null,                       // deletedAt
                        ModerationStatus.PUBLISHED, // état "par défaut"
                        0L                          // version initiale (ajuste à 1L si tu décides d'incrémenter à la création)
                )
        );
    }
}
