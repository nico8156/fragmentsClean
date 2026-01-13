package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringCommentRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.CommentJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WriteCommentControllerIT extends AbstractBaseE2E {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID COMMENT_ID = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
	private static final UUID USER_ID = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
	private static final UUID TARGET_ID = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private SpringCommentRepository springCommentRepository;
	@Autowired
	private SpringOutboxEventRepository outboxRepository;
	@Autowired
	private DateTimeProvider dateTimeProvider;

	@BeforeEach
	void setup() {
		springCommentRepository.deleteAll();
		outboxRepository.deleteAll();

		// si ton deterministic est bien câblé ici, sinon on fera comme pour Like
		// (assert sans updatedAt)
		((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow = Instant.parse("2024-01-01T10:00:00Z");
	}

	@Test
	void can_create_comment_and_persist_outbox_event() throws Exception {
		var clientAt = "2024-01-01T09:00:00Z";

		mockMvc.perform(
				post("/api/social/comments")
						.with(jwt().jwt(j -> j
								.subject(USER_ID.toString())
								.claim("roles", List.of("USER"))))
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

		var now = Instant.parse("2023-10-01T11:00:00Z");

		// 1) Write model
		assertThat(springCommentRepository.findAll()).containsExactly(
				new CommentJpaEntity(
						COMMENT_ID,
						TARGET_ID,
						USER_ID,
						null,
						"Hello world",
						now,
						null,
						null,
						ModerationStatus.PUBLISHED,
						0L));

		// 2) Outbox
		var outboxEvents = outboxRepository.findAll();
		assertThat(outboxEvents).hasSize(1);

		OutboxEventJpaEntity event = outboxEvents.get(0);

		assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
		assertThat(event.getEventType()).isEqualTo(CommentCreatedEvent.class.getName());
		assertThat(event.getAggregateType()).isEqualTo("Comment");
		assertThat(event.getAggregateId()).isEqualTo(COMMENT_ID.toString());

		// ⚠️ aligné avec Like: WS par user => streamKey user:<userId>
		assertThat(event.getStreamKey()).isEqualTo("user:" + USER_ID);

		assertThat(event.getPayloadJson()).isNotBlank();
	}
}
