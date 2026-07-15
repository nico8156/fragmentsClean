package com.nm.fragmentsclean.userApplicationContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.SpringSavedCoffeeRepository;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.SavedCoffeeJpaEntity;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffeeSetEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WriteSavedCoffeeControllerIT extends AbstractBaseE2E {
	private static final UUID COMMAND_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
	private static final UUID SAVED_COFFEE_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
	private static final UUID USER_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
	private static final UUID COFFEE_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SpringSavedCoffeeRepository savedCoffeeRepository;

	@Autowired
	private SpringOutboxEventRepository outboxRepository;

	@Autowired
	private DateTimeProvider dateTimeProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM saved_coffees");
		jdbcTemplate.update("DELETE FROM app_users");
		jdbcTemplate.update("DELETE FROM auth_users");
		outboxRepository.deleteAll();
		jdbcTemplate.update("DELETE FROM command_status");
		((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow = Instant.parse("2023-10-01T11:00:00Z");
		seedUser(USER_ID);
	}

	@Test
	void can_save_coffee_and_persist_outbox_event() throws Exception {
		mockMvc.perform(post("/api/users/me/saved-coffees")
						.with(jwt().jwt(j -> j.subject(USER_ID.toString()).claim("roles", java.util.List.of("USER"))))
						.contentType("application/json")
						.content("""
								{
								  "commandId": "%s",
								  "savedCoffeeId": "%s",
								  "coffeeId": "%s",
								  "value": true,
								  "at": "2026-01-01T09:59:00.000Z"
								}
								""".formatted(COMMAND_ID, SAVED_COFFEE_ID, COFFEE_ID)))
				.andExpect(status().isAccepted());

		assertThat(savedCoffeeRepository.findAll()).containsExactly(
				new SavedCoffeeJpaEntity(
						SAVED_COFFEE_ID,
						USER_ID,
						COFFEE_ID,
						true,
						Instant.parse("2023-10-01T11:00:00Z"),
						1L));

		var outboxEvents = outboxRepository.findAll();
		assertThat(outboxEvents).hasSize(1);
		OutboxEventJpaEntity event = outboxEvents.get(0);
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
		assertThat(event.getEventType()).isEqualTo(SavedCoffeeSetEvent.class.getName());
		assertThat(event.getAggregateType()).isEqualTo("SavedCoffee");
		assertThat(event.getAggregateId()).isEqualTo(SAVED_COFFEE_ID.toString());
		assertThat(event.getStreamKey()).isEqualTo("appUser:" + USER_ID + ":savedCoffee:" + SAVED_COFFEE_ID);

		String commandStatus = jdbcTemplate.queryForObject(
				"SELECT status FROM command_status WHERE command_id = ?",
				String.class,
				COMMAND_ID);
		assertThat(commandStatus).isEqualTo("APPLIED");
	}

	private void seedUser(UUID userId) {
		jdbcTemplate.update("""
				INSERT INTO auth_users (
				  id, provider, provider_user_id, email, email_verified, display_name, avatar_url, last_login_at
				)
				VALUES (?, 'GOOGLE', ?, 'user@example.test', true, 'User', null, ?)
				""",
				userId,
				userId.toString(),
				java.sql.Timestamp.from(Instant.parse("2026-01-01T09:00:00Z")));
		jdbcTemplate.update("""
				INSERT INTO app_users (
				  id, auth_user_id, display_name, avatar_url, created_at, updated_at, version
				)
				VALUES (?, ?, 'User', null, ?, ?, 0)
				""",
				userId,
				userId,
				java.sql.Timestamp.from(Instant.parse("2026-01-01T09:00:00Z")),
				java.sql.Timestamp.from(Instant.parse("2026-01-01T09:00:00Z")));
	}
}
