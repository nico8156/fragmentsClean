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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ActiveProfiles("auth_test")
public class WriteLikeControllerIT extends AbstractBaseE2E {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID LIKE_ID = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
	private static final UUID USER_ID = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
	private static final UUID TARGET_ID = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SpringLikeRepository springLikeRepository;

	@Autowired
	private SpringOutboxEventRepository outboxRepository;

	@Autowired
	private DateTimeProvider dateTimeProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setup() {
		springLikeRepository.deleteAll();
		outboxRepository.deleteAll();
		jdbcTemplate.update("DELETE FROM command_status");

		((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow = Instant.parse("2023-10-01T11:00:00Z");
	}

	@Test
	void can_set_like_active_true_and_persist_outbox_event() throws Exception {
		var now = Instant.parse("2023-10-01T11:00:00Z");
		var clientAt = "2025-11-25T10:15:30.000Z";

		mockMvc.perform(
				post("/api/social/likes")
						.with(jwt().jwt(j -> j
								.subject(USER_ID.toString())
								.claim("roles", java.util.List.of("USER"))))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "likeId": "%s",
								  "targetId": "%s",
								  "value": %s,
								  "at": "%s"
								}
								""".formatted(
								COMMAND_ID,
								LIKE_ID,
								TARGET_ID,
								true,
								clientAt)))
				.andExpect(status().isAccepted());

		// 1) Write model
		assertThat(springLikeRepository.findAll()).containsExactly(
				new LikeJpaEntity(
						LIKE_ID,
						USER_ID,
						TARGET_ID,
						true,
						now,
						1L));

		// 2) Outbox
		var outboxEvents = outboxRepository.findAll();
		assertThat(outboxEvents).hasSize(1);

		OutboxEventJpaEntity event = outboxEvents.get(0);

		assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
		assertThat(event.getEventType()).isEqualTo(LikeSetEvent.class.getName());
		assertThat(event.getAggregateType()).isEqualTo("Like");
		assertThat(event.getAggregateId()).isEqualTo(LIKE_ID.toString());
		assertThat(event.getStreamKey()).isEqualTo("user:" + USER_ID);
		assertThat(event.getPayloadJson()).isNotBlank();

		String commandStatus = jdbcTemplate.queryForObject(
				"SELECT status FROM command_status WHERE command_id = ?",
				String.class,
				COMMAND_ID);
		assertThat(commandStatus).isEqualTo("APPLIED");
	}

	@Test
	void like_set_is_idempotent_when_sent_twice() throws Exception {
		var clientAt = "2025-11-25T10:15:30.000Z";

		var body = """
				{
				  "commandId": "%s",
				  "likeId": "%s",
				  "targetId": "%s",
				  "value": true,
				  "at": "%s"
				}
				""".formatted(COMMAND_ID, LIKE_ID, TARGET_ID, clientAt);

		var auth = jwt().jwt(j -> j
				.subject(USER_ID.toString())
				.claim("roles", java.util.List.of("USER")));

		mockMvc.perform(post("/api/social/likes").with(auth)
				.contentType("application/json").content(body))
				.andExpect(status().isAccepted());

		mockMvc.perform(post("/api/social/likes").with(auth)
				.contentType("application/json").content(body))
				.andExpect(status().isAccepted());

		// THEN: 1 row only
		var rows = springLikeRepository.findAll();
		assertThat(rows).hasSize(1);

		// Use equals/hashCode-based comparison as you already do, but avoid asserting
		// updatedAt strict
		var saved = rows.get(0);
		assertThat(saved)
				.usingRecursiveComparison()
				.ignoringFields("updatedAt")
				.isEqualTo(new LikeJpaEntity(LIKE_ID, USER_ID, TARGET_ID, true, null, 1L));

		assertThat(outboxRepository.findAll()).hasSize(1);

		String commandStatus = jdbcTemplate.queryForObject(
				"SELECT status FROM command_status WHERE command_id = ?",
				String.class,
				COMMAND_ID);
		assertThat(commandStatus).isEqualTo("APPLIED");
	}

	@Test
	void command_status_is_visible_only_to_the_authenticated_requester() throws Exception {
		postLike(COMMAND_ID, LIKE_ID, TARGET_ID, USER_ID, true)
				.andExpect(status().isAccepted());

		mockMvc.perform(get("/commands/{commandId}", COMMAND_ID)
					.with(userJwt(USER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPLIED"));

		UUID anotherUser = UUID.fromString("77777777-7777-4777-8777-777777777777");
		mockMvc.perform(get("/commands/{commandId}", COMMAND_ID)
					.with(userJwt(anotherUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.reason").doesNotExist());
	}

	@Test
	void explicit_business_rejection_is_persisted_and_replayed() throws Exception {
		springLikeRepository.save(new LikeJpaEntity(
				LIKE_ID, USER_ID, TARGET_ID, true, Instant.parse("2026-09-11T10:00:00Z"), 1L));
		UUID anotherUser = UUID.fromString("77777777-7777-4777-8777-777777777777");
		UUID rejectedCommand = UUID.fromString("88888888-8888-4888-8888-888888888888");

		postLike(rejectedCommand, LIKE_ID, TARGET_ID, anotherUser, false)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error").value("COMMAND_REJECTED"))
				.andExpect(jsonPath("$.reason").value("LIKE_ID_CONFLICT"));

		mockMvc.perform(get("/commands/{commandId}", rejectedCommand)
					.with(userJwt(anotherUser)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.rejectionCode").value("LIKE_ID_CONFLICT"));

		postLike(rejectedCommand, LIKE_ID, TARGET_ID, anotherUser, false)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.reason").value("LIKE_ID_CONFLICT"));
	}

	@Test
	void command_id_reuse_with_another_intent_is_rejected_without_second_effect() throws Exception {
		postLike(COMMAND_ID, LIKE_ID, TARGET_ID, USER_ID, true)
				.andExpect(status().isAccepted());

		postLike(COMMAND_ID, LIKE_ID, TARGET_ID, USER_ID, false)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("COMMAND_ID_CONFLICT"))
				.andExpect(jsonPath("$.reason").value("COMMAND_ID_REUSED"));

		assertThat(springLikeRepository.findById(LIKE_ID).orElseThrow().isActive()).isTrue();
		assertThat(outboxRepository.findAll()).hasSize(1);
	}

	@Test
	void unknown_and_legacy_ownerless_receipts_are_indistinguishable_to_mobile() throws Exception {
		UUID legacyCommand = UUID.fromString("99999999-9999-4999-8999-999999999999");
		jdbcTemplate.update("""
				INSERT INTO command_status(command_id, status, applied_at, updated_at)
				VALUES (?, 'APPLIED', now(), now())
				""", legacyCommand);

		for (UUID id : java.util.List.of(legacyCommand, UUID.randomUUID())) {
			mockMvc.perform(get("/commands/{commandId}", id).with(userJwt(USER_ID)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("PENDING"));
		}
	}

	private org.springframework.test.web.servlet.ResultActions postLike(
			UUID commandId, UUID likeId, UUID targetId, UUID userId, boolean value) throws Exception {
		return mockMvc.perform(post("/api/social/likes")
				.with(userJwt(userId))
				.contentType("application/json")
				.content("""
						{
						  "commandId": "%s",
						  "likeId": "%s",
						  "targetId": "%s",
						  "value": %s,
						  "at": "2026-09-11T10:00:00Z"
						}
						""".formatted(commandId, likeId, targetId, value)));
	}

	private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt(UUID userId) {
		return jwt().jwt(j -> j.subject(userId.toString()).claim("roles", java.util.List.of("USER")));
	}

}
