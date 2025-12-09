package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.LikeJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// imports junit / mockmvc etc...

public class WriteLikeControllerIT extends AbstractBaseE2E {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID LIKE_ID    = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
    private static final UUID USER_ID    = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
    private static final UUID TARGET_ID  = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringLikeRepository springLikeRepository;

    @Autowired
    private SpringOutboxEventRepository outboxRepository;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @BeforeEach
    void setup() {
        springLikeRepository.deleteAll();
        outboxRepository.deleteAll();

        ((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow =
                Instant.parse("2024-01-01T10:00:00Z");
    }

    @Test
    void can_set_like_active_true_and_persist_outbox_event() throws Exception {
        var clientAt = "2024-01-01T09:00:00Z";

        mockMvc.perform(
                        post("/api/social/likes")
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "commandId": "%s",
                                          "likeId": "%s",
                                          "userId": "%s",
                                          "targetId": "%s",
                                          "value": true,
                                          "at": "%s"
                                        }
                                        """.formatted(
                                                COMMAND_ID,
                                                LIKE_ID,
                                                USER_ID,
                                                TARGET_ID,
                                                clientAt
                                        )
                                )
                )
                .andExpect(status().isAccepted());

        var now = Instant.parse("2024-01-01T10:00:00Z");

        // 1) Write model
        assertThat(springLikeRepository.findAll()).containsExactly(
                new LikeJpaEntity(
                        LIKE_ID,
                        USER_ID,
                        TARGET_ID,
                        true,
                        now,
                        1L
                )
        );

        // 2) Outbox
        var outboxEvents = outboxRepository.findAll();
        assertThat(outboxEvents).hasSize(1);

        OutboxEventJpaEntity event = outboxEvents.get(0);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getEventType()).isEqualTo(LikeSetEvent.class.getName());
        assertThat(event.getAggregateType()).isEqualTo("Like");
        assertThat(event.getAggregateId()).isEqualTo(LIKE_ID.toString());
        assertThat(event.getStreamKey()).isEqualTo("social:" + TARGET_ID);
        assertThat(event.getPayloadJson()).isNotBlank();
    }
}
