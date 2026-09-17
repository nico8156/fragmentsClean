package com.nm.fragmentsclean.sharedKernel.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.InboxMessageRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.InboxClaim;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;

class InboxMessageRepositoryTest {
	@Test
	void processed_duplicate_is_suppressed() {
		var jdbc = new DuplicateInboxJdbcTemplate("PROCESSED", false);
		var repository = new InboxMessageRepository(jdbc);

		var claimed = repository.claim(envelope());

		assertThat(claimed.status()).isEqualTo(InboxClaim.Status.ALREADY_PROCESSED);
		assertThat(jdbc.resetCalls).isEqualTo(1);
	}

	@Test
	void failed_duplicate_is_claimed_again_for_redelivery() {
		var jdbc = new DuplicateInboxJdbcTemplate("FAILED", false);
		var repository = new InboxMessageRepository(jdbc);

		var claimed = repository.claim(envelope());

		assertThat(claimed.acquired()).isTrue();
		assertThat(jdbc.resetCalls).isEqualTo(1);
		assertThat(jdbc.sqlCalls).anyMatch(sql -> sql.contains("SET status = 'RECEIVED'"));
	}

	@Test
	void active_received_duplicate_is_suppressed() {
		var jdbc = new DuplicateInboxJdbcTemplate("RECEIVED", false);
		var repository = new InboxMessageRepository(jdbc);

		var claimed = repository.claim(envelope());

		assertThat(claimed.status()).isEqualTo(InboxClaim.Status.BUSY);
		assertThat(jdbc.resetCalls).isEqualTo(1);
	}

	@Test
	void received_duplicate_is_claimed_again_after_lease_expiry() {
		var jdbc = new DuplicateInboxJdbcTemplate("RECEIVED", true);
		assertThat(new InboxMessageRepository(jdbc).claim(envelope()).acquired()).isTrue();
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
		private final boolean expired;
		private final List<String> sqlCalls = new ArrayList<>();
		private int resetCalls;

		private DuplicateInboxJdbcTemplate(String existingStatus, boolean expired) {
			this.existingStatus = existingStatus;
			this.expired = expired;
		}

		@Override
		public int update(String sql, Object... args) {
			sqlCalls.add(sql);
			if (sql.contains("INSERT INTO inbox_messages")) {
				throw new DuplicateKeyException("duplicate");
			}
			if (sql.contains("SET status = 'RECEIVED'")) {
				resetCalls++;
				return "FAILED".equals(existingStatus) || expired ? 1 : 0;
			}
			return 1;
		}

		@Override
		public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
			try {
				ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(
						ResultSet.class.getClassLoader(),
						new Class<?>[]{ResultSet.class},
						(proxy, method, methodArgs) -> switch (method.getName()) {
							case "getString" -> existingStatus;
							case "getTimestamp" -> Timestamp.from(Instant.parse("2026-07-05T10:05:00Z"));
							case "wasNull", "isClosed" -> false;
							case "close" -> null;
							default -> throw new UnsupportedOperationException(method.getName());
						});
				return List.of(rowMapper.mapRow(resultSet, 0));
			} catch (Exception exception) {
				throw new IllegalStateException(exception);
			}
		}
	}
}
