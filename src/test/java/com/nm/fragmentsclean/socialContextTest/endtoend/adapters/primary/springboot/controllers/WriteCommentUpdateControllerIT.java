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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WriteCommentUpdateControllerIT extends AbstractBaseE2E {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID COMMENT_ID = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
	private static final UUID USER_ID = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
	private static final UUID TARGET_ID = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SpringCommentRepository springCommentRepository;

	@Autowired
	private DateTimeProvider dateTimeProvider;

	private static RequestPostProcessor authUser(UUID userId) {
		return jwt().jwt(j -> j
				.subject(userId.toString())
				.claim("roles", List.of("USER")));
	}

	@BeforeEach
	void setup() {
		springCommentRepository.deleteAll();

		((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow = Instant.parse("2024-01-01T10:00:00Z");
	}

	@Test
	void can_update_comment_body() throws Exception {
		// given
		var createdAt = Instant.parse("2023-10-01T11:00:00Z");

		springCommentRepository.save(
				new CommentJpaEntity(
						COMMENT_ID,
						TARGET_ID,
						USER_ID,
						null,
						"Old body",
						createdAt,
						null,
						null,
						ModerationStatus.PUBLISHED,
						0L));

		var clientEditedAt = "2024-01-01T09:00:00Z";

		// when
		mockMvc.perform(
				put("/api/social/comments")
						.with(authUser(USER_ID))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "commentId": "%s",
								  "body": "New body",
								  "editedAt": "%s"
								}
								""".formatted(
								COMMAND_ID,
								COMMENT_ID,
								clientEditedAt)))
				.andExpect(status().isAccepted());

		// then
		var entities = springCommentRepository.findAll();
		assertThat(entities).hasSize(1);
		// TODO refaire un point sur le provider de determistic date ...
		var now = Instant.parse("2023-10-01T11:00:00Z");

		assertThat(entities.get(0))
				.usingRecursiveComparison()
				.isEqualTo(new CommentJpaEntity(
						COMMENT_ID,
						TARGET_ID,
						USER_ID,
						null,
						"New body",
						createdAt,
						now, // editedAt = serverNow
						null,
						ModerationStatus.PUBLISHED,
						1L));
	}
}
