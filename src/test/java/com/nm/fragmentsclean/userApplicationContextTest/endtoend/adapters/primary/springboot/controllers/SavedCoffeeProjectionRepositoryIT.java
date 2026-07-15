package com.nm.fragmentsclean.userApplicationContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.userApplicationContext.read.adapters.secondary.repositories.JdbcSavedCoffeeProjectionRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffeeSetEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SavedCoffeeProjectionRepositoryIT extends AbstractBaseE2E {
	private static final UUID SAVED_COFFEE_ID = UUID.fromString("77777777-7777-4777-8777-777777777777");
	private static final UUID USER_ID = UUID.fromString("88888888-8888-4888-8888-888888888888");
	private static final UUID COFFEE_ID = UUID.fromString("99999999-9999-4999-8999-999999999999");

	@Autowired
	private JdbcSavedCoffeeProjectionRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM user_saved_coffees_projection");
	}

	@Test
	void older_replay_does_not_overwrite_newer_projection() {
		repository.apply(event(true, 2, "2026-01-01T10:00:00Z"));
		repository.apply(event(false, 1, "2026-01-01T09:00:00Z"));

		Boolean active = jdbcTemplate.queryForObject("""
				SELECT active
				FROM user_saved_coffees_projection
				WHERE saved_coffee_id = ?
				""",
				Boolean.class,
				SAVED_COFFEE_ID);

		assertThat(active).isTrue();
	}

	private SavedCoffeeSetEvent event(boolean active, long version, String occurredAt) {
		return new SavedCoffeeSetEvent(
				UUID.randomUUID(),
				UUID.randomUUID(),
				SAVED_COFFEE_ID,
				USER_ID,
				COFFEE_ID,
				active,
				version,
				Instant.parse(occurredAt),
				Instant.parse(occurredAt));
	}
}
