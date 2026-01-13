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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SocialLikeOutboxRoutingIT extends AbstractBaseE2E {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID LIKE_ID = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
	private static final UUID USER_ID = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
	private static final UUID TARGET_ID = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");

	private static final String LIKE_SET_EVENT = "com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent";

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

	private static RequestPostProcessor authUser(UUID userId) {
		return jwt().jwt(j -> j
				.subject(userId.toString())
				// IMPORTANT: ton JwtAuthConverter lit "roles"
				.claim("roles", List.of("USER")));
	}

	@Test
	void setting_like_routes_outbox_event_to_kafka_and_websocket() throws Exception {
		var clientAt = "2024-01-01T09:00:00Z";

		// GIVEN : like via l’API write (JWT requis + DTO sans userId)
		mockMvc.perform(
				post("/api/social/likes")
						.with(authUser(USER_ID))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "likeId": "%s",
								  "targetId": "%s",
								  "value": true,
								  "at": "%s"
								}
								""".formatted(
								COMMAND_ID,
								LIKE_ID,
								TARGET_ID,
								clientAt)))
				.andExpect(status().isAccepted());

		// Sanity check : il y a au moins 1 event en outbox en PENDING
		List<OutboxEventJpaEntity> pendingBefore = outboxEventRepository.findAll();
		assertThat(pendingBefore).isNotEmpty();
		assertThat(pendingBefore).anySatisfy(e -> assertThat(e.getStatus()).isEqualTo(OutboxStatus.PENDING));

		// WHEN : on déclenche le dispatcher
		outboxEventDispatcher.dispatchPending();

		// THEN : l'event LikeSet est marqué SENT et a streamKey user:<userId>
		List<OutboxEventJpaEntity> after = outboxEventRepository.findAll();
		assertThat(after).isNotEmpty();
		assertThat(after).anySatisfy(e -> {
			if (LIKE_SET_EVENT.equals(e.getEventType())) {
				assertThat(e.getStatus()).isEqualTo(OutboxStatus.SENT);
				assertThat(e.getStreamKey()).isEqualTo("user:" + USER_ID);
				assertThat(e.getAggregateId()).isEqualTo(LIKE_ID.toString());
			}
		});

		// Capture Kafka
		ArgumentCaptor<OutboxEventJpaEntity> kafkaCaptor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
		verify(kafkaOutboxEventSender, atLeastOnce()).send(kafkaCaptor.capture());

		var kafkaEvents = kafkaCaptor.getAllValues();
		assertThat(kafkaEvents).anySatisfy(e -> {
			assertThat(e.getEventType()).isEqualTo(LIKE_SET_EVENT);
			assertThat(e.getStreamKey()).isEqualTo("user:" + USER_ID);
			assertThat(e.getAggregateId()).isEqualTo(LIKE_ID.toString());
		});

		// Capture WebSocket
		ArgumentCaptor<OutboxEventJpaEntity> wsCaptor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
		verify(webSocketOutboxEventSender, atLeastOnce()).send(wsCaptor.capture());

		var wsEvents = wsCaptor.getAllValues();
		assertThat(wsEvents).anySatisfy(e -> {
			assertThat(e.getEventType()).isEqualTo(LIKE_SET_EVENT);
			assertThat(e.getStreamKey()).isEqualTo("user:" + USER_ID);
			assertThat(e.getAggregateId()).isEqualTo(LIKE_ID.toString());
		});
	}
}
