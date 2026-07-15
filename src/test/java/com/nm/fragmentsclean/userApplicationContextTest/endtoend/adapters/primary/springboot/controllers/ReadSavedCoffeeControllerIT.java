package com.nm.fragmentsclean.userApplicationContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReadSavedCoffeeControllerIT extends AbstractBaseE2E {
	private static final UUID USER_ID = UUID.fromString("55555555-5555-4555-8555-555555555555");
	private static final UUID COFFEE_ID = UUID.fromString("66666666-6666-4666-8666-666666666666");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DateTimeProvider dateTimeProvider;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM user_saved_coffees_projection");
		jdbcTemplate.update("DELETE FROM user_saved_coffee_cafes_projection");
		((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow = Instant.parse("2023-10-01T11:00:00Z");

		jdbcTemplate.update("""
				INSERT INTO user_saved_coffee_cafes_projection (
				  coffee_id, name, address_line1, city, postal_code, country, archived, version, updated_at
				)
				VALUES (?, 'Fragments République', '10 rue Test', 'Paris', '75011', 'FR', false, 2, ?)
				""",
				COFFEE_ID,
				Timestamp.from(Instant.parse("2026-01-01T09:00:00Z")));
		jdbcTemplate.update("""
				INSERT INTO user_saved_coffees_projection (
				  saved_coffee_id, user_id, coffee_id, active, version, updated_at
				)
				VALUES (?, ?, ?, true, 3, ?)
				""",
				UUID.randomUUID(),
				USER_ID,
				COFFEE_ID,
				Timestamp.from(Instant.parse("2026-01-01T09:30:00Z")));
	}

	@Test
	void can_read_saved_coffees_for_current_user() throws Exception {
		mockMvc.perform(get("/api/users/me/saved-coffees")
						.with(jwt().jwt(j -> j.subject(USER_ID.toString()).claim("roles", java.util.List.of("USER")))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].coffeeId", is(COFFEE_ID.toString())))
				.andExpect(jsonPath("$.items[0].name", is("Fragments République")))
				.andExpect(jsonPath("$.items[0].city", is("Paris")))
				.andExpect(jsonPath("$.version", is(3)))
				.andExpect(jsonPath("$.serverTime", is("2023-10-01T11:00:00Z")));
	}
}
