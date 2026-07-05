package com.nm.fragmentsclean.sharedKernel.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.InboxMessageRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;

class InboxMessageRepositoryTest {
	@Test
	void processed_duplicate_is_suppressed() {
		var jdbc = new DuplicateInboxJdbcTemplate("PROCESSED");
		var repository = new InboxMessageRepository(jdbc);

		boolean claimed = repository.claim(envelope());

		assertThat(claimed).isFalse();
		assertThat(jdbc.resetCalls).isZero();
	}

	@Test
	void failed_duplicate_is_claimed_again_for_redelivery() {
		var jdbc = new DuplicateInboxJdbcTemplate("FAILED");
		var repository = new InboxMessageRepository(jdbc);

		boolean claimed = repository.claim(envelope());

		assertThat(claimed).isTrue();
		assertThat(jdbc.resetCalls).isEqualTo(1);
		assertThat(jdbc.sqlCalls).anyMatch(sql -> sql.contains("SET status = 'RECEIVED'"));
	}

	@Test
	void received_duplicate_is_claimed_again_after_visibility_timeout_redelivery() {
		var jdbc = new DuplicateInboxJdbcTemplate("RECEIVED");
		var repository = new InboxMessageRepository(jdbc);

		boolean claimed = repository.claim(envelope());

		assertThat(claimed).isTrue();
		assertThat(jdbc.resetCalls).isEqualTo(1);
	}

	private static IntegrationEventEnvelope envelope() {
		return new IntegrationEventEnvelope(
				"event-1",
				"coffee.created",
				1,
				"com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent",
				"Coffee",
				"coffee-1",
				"coffee:coffee-1",
				"coffees-events",
				"{}",
				Instant.parse("2026-07-05T10:00:00Z"));
	}

	private static class DuplicateInboxJdbcTemplate extends JdbcTemplate {
		private final String existingStatus;
		private final List<String> sqlCalls = new ArrayList<>();
		private int resetCalls;

		private DuplicateInboxJdbcTemplate(String existingStatus) {
			this.existingStatus = existingStatus;
		}

		@Override
		public int update(String sql, Object... args) {
			sqlCalls.add(sql);
			if (sql.contains("INSERT INTO inbox_messages")) {
				throw new DuplicateKeyException("duplicate");
			}
			if (sql.contains("SET status = 'RECEIVED'")) {
				resetCalls++;
			}
			return 1;
		}

		@Override
		public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
			sqlCalls.add(sql);
			return requiredType.cast(existingStatus);
		}
	}
}
