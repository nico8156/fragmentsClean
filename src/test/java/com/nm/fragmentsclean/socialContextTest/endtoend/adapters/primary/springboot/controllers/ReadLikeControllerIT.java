//TODO ce test est OK 
package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ReadLikeControllerIT extends AbstractBaseE2E {

	private static final UUID TARGET_ID = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
	private static final UUID ME_USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DateTimeProvider dateTimeProvider;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM social_likes_projection");

		var now = Instant.parse("2024-01-01T10:00:00Z");

		((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow = now;

		// "me" a liké
		insertProjection(
				UUID.randomUUID(),
				ME_USER,
				TARGET_ID,
				true,
				now,
				3L);

		// un autre a liké
		insertProjection(
				UUID.randomUUID(),
				OTHER,
				TARGET_ID,
				true,
				now,
				3L);

		// like inactif sur ce target (ne compte pas)
		insertProjection(
				UUID.randomUUID(),
				UUID.randomUUID(),
				TARGET_ID,
				false,
				now,
				3L);
	}

	@Test
	void can_read_like_status_for_target() throws Exception {
		mockMvc.perform(
				get("/api/social/targets/{targetId}/likes", TARGET_ID))
				.andExpect(status().isOk())
					.andExpect(content().contentType("application/json"))
					.andExpect(jsonPath("$.count", is(2)))
					.andExpect(jsonPath("$.me", is(true)))
					.andExpect(jsonPath("$.version", is(3)))
					.andExpect(jsonPath("$.serverTime", notNullValue()));
	}

	private void insertProjection(UUID likeId, UUID userId, UUID targetId, boolean active, Instant updatedAt, long version) {
		jdbcTemplate.update("""
				INSERT INTO social_likes_projection (
				  like_id, user_id, target_id, active, updated_at, version
				)
				VALUES (?, ?, ?, ?, ?, ?)
				""",
				likeId,
				userId,
				targetId,
				active,
				updatedAt,
				version);
	}
}
