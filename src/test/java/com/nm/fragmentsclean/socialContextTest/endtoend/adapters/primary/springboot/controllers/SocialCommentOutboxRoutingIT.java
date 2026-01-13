package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.KafkaOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.WebSocketOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;

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

/**
 * Flow :
 * POST /api/social/comments
 * → CommentCreatedEvent
 * → OutboxDomainEventPublisher (PENDING)
 * → OutboxEventDispatcher.dispatchPending()
 * → RoutingOutboxEventSender
 * → KafkaOutboxEventSender
 * → WebSocketOutboxEventSender
 */
public class SocialCommentOutboxRoutingIT extends AbstractBaseE2E {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID COMMENT_ID = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
	private static final UUID USER_ID = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
	private static final UUID TARGET_ID = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");

	private static final String COMMENT_CREATED_EVENT = "com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent";

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

	private static RequestPostProcessor authUser(UUID userId) {
		return jwt().jwt(j -> j
				.subject(userId.toString())
				.claim("roles", List.of("USER")));
	}

	@BeforeEach
	void setup() {
		outboxEventRepository.deleteAll();
	}

	@Test
	void creating_comment_routes_outbox_event_to_kafka_and_websocket() throws Exception {
		var clientAt = "2024-01-01T09:00:00Z";

		// GIVEN: create comment (JWT=sub; body sans userId)
		mockMvc.perform(
				post("/api/social/comments")
						.with(authUser(USER_ID))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "commentId": "%s",
								  "targetId": "%s",
								  "parentId": null,
								  "body": "Hello world",
								  "at": "%s"
								}
								""".formatted(
								COMMAND_ID,
								COMMENT_ID,
								TARGET_ID,
								clientAt)))
				.andExpect(status().isAccepted());

		// Sanity: outbox not empty
		var outboxBefore = outboxEventRepository.findAll();
		assertThat(outboxBefore).isNotEmpty();

		// WHEN
		outboxEventDispatcher.dispatchPending();

		// THEN: capture WS sends
		ArgumentCaptor<OutboxEventJpaEntity> wsCaptor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
		verify(webSocketOutboxEventSender, atLeastOnce()).send(wsCaptor.capture());

		var wsEvents = wsCaptor.getAllValues();
		assertThat(wsEvents)
				.anySatisfy(e -> {
					assertThat(e.getEventType()).isEqualTo(COMMENT_CREATED_EVENT);
					// abonnement WS par user
					assertThat(e.getStreamKey()).isEqualTo("user:" + USER_ID);
					assertThat(e.getAggregateId()).isEqualTo(COMMENT_ID.toString());
				});

		// THEN: capture Kafka sends
		ArgumentCaptor<OutboxEventJpaEntity> kafkaCaptor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
		verify(kafkaOutboxEventSender, atLeastOnce()).send(kafkaCaptor.capture());

		var kafkaEvents = kafkaCaptor.getAllValues();
		assertThat(kafkaEvents)
				.anySatisfy(e -> {
					assertThat(e.getEventType()).isEqualTo(COMMENT_CREATED_EVENT);
					assertThat(e.getStreamKey()).isEqualTo("user:" + USER_ID);
					assertThat(e.getAggregateId()).isEqualTo(COMMENT_ID.toString());
				});
	}
}
