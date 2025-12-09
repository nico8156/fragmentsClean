package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.KafkaOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.WebSocketOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SocialLikeOutboxRoutingIT extends AbstractBaseE2E {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID LIKE_ID    = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
    private static final UUID USER_ID    = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
    private static final UUID TARGET_ID  = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringOutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventDispatcher outboxEventDispatcher;

    @SpyBean
    private KafkaOutboxEventSender kafkaOutboxEventSender;

    @SpyBean
    private WebSocketOutboxEventSender webSocketOutboxEventSender;

    @BeforeEach
    void setup() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void setting_like_routes_outbox_event_to_kafka_and_websocket() throws Exception {
        var clientAt = "2024-01-01T09:00:00Z";

        // GIVEN : like via l’API write
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

        // Sanity check : il y a au moins 1 event en outbox
        List<OutboxEventJpaEntity> pendingBefore = outboxEventRepository.findAll();
        assertThat(pendingBefore).isNotEmpty();
        assertThat(pendingBefore)
                .anySatisfy(e -> assertThat(e.getStatus()).isEqualTo(OutboxStatus.PENDING));

        // WHEN : on déclenche le dispatcher
        outboxEventDispatcher.dispatchPending();

        // THEN : les events ont été marqués SENT
        List<OutboxEventJpaEntity> after = outboxEventRepository.findAll();
        assertThat(after).isNotEmpty();
        assertThat(after)
                .anySatisfy(e -> {
                    assertThat(e.getStatus()).isEqualTo(OutboxStatus.SENT);
                    // notre like social doit avoir ce streamKey
                    if (e.getEventType().equals(
                            "com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent"
                    )) {
                        assertThat(e.getStreamKey()).isEqualTo("social:" + TARGET_ID);
                        assertThat(e.getAggregateId()).isEqualTo(LIKE_ID.toString());
                    }
                });

        // On capture tous les appels Kafka
        ArgumentCaptor<OutboxEventJpaEntity> kafkaCaptor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(kafkaOutboxEventSender, atLeastOnce()).send(kafkaCaptor.capture());

        var kafkaEvents = kafkaCaptor.getAllValues();

        assertThat(kafkaEvents)
                .anySatisfy(e -> {
                    assertThat(e.getEventType())
                            .isEqualTo("com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent");
                    assertThat(e.getStreamKey())
                            .isEqualTo("social:" + TARGET_ID);
                    assertThat(e.getAggregateId())
                            .isEqualTo(LIKE_ID.toString());
                });

        // On capture tous les appels WebSocket
        ArgumentCaptor<OutboxEventJpaEntity> wsCaptor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(webSocketOutboxEventSender, atLeastOnce()).send(wsCaptor.capture());

        var wsEvents = wsCaptor.getAllValues();

        assertThat(wsEvents)
                .anySatisfy(e -> {
                    assertThat(e.getEventType())
                            .isEqualTo("com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent");
                    assertThat(e.getStreamKey())
                            .isEqualTo("social:" + TARGET_ID);
                    assertThat(e.getAggregateId())
                            .isEqualTo(LIKE_ID.toString());
                });
    }
}
