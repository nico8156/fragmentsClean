package com.nm.fragmentsclean.userApplicationContextTest.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.SavedCoffeeRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffee;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffeeSetEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.SetSavedCoffeeCommand;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.SetSavedCoffeeCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class SetSavedCoffeeCommandHandlerTest {
	private final UUID SAVED_COFFEE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
	private final UUID USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
	private final UUID COFFEE_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");
	private final UUID COMMAND_ID = UUID.fromString("44444444-4444-4444-8444-444444444444");

	private FakeSavedCoffeeRepository repository;
	private FakeDomainEventPublisher eventPublisher;
	private RecordingCommandStatusRecorder commandStatusRecorder;
	private SetSavedCoffeeCommandHandler handler;

	@BeforeEach
	void setup() {
		repository = new FakeSavedCoffeeRepository();
		eventPublisher = new FakeDomainEventPublisher();
		commandStatusRecorder = new RecordingCommandStatusRecorder();
		handler = new SetSavedCoffeeCommandHandler(
				repository,
				eventPublisher,
				new DeterministicDateTimeProvider(),
				commandStatusRecorder);
	}

	@Test
	void saves_coffee_and_publishes_projection_event() {
		handler.execute(new SetSavedCoffeeCommand(
				COMMAND_ID.toString(),
				SAVED_COFFEE_ID,
				USER_ID,
				COFFEE_ID,
				true,
				Instant.parse("2026-01-01T09:59:00Z")));

		var snapshot = repository.allSnapshots().getFirst();
		assertThat(snapshot.savedCoffeeId()).isEqualTo(SAVED_COFFEE_ID);
		assertThat(snapshot.userId()).isEqualTo(USER_ID);
		assertThat(snapshot.coffeeId()).isEqualTo(COFFEE_ID);
		assertThat(snapshot.active()).isTrue();
		assertThat(snapshot.version()).isEqualTo(1L);

		assertThat(eventPublisher.published).hasSize(1);
		var event = (SavedCoffeeSetEvent) eventPublisher.published.getFirst();
		assertThat(event.commandId()).isEqualTo(COMMAND_ID);
		assertThat(event.savedCoffeeId()).isEqualTo(SAVED_COFFEE_ID);
		assertThat(event.userId()).isEqualTo(USER_ID);
		assertThat(event.coffeeId()).isEqualTo(COFFEE_ID);
		assertThat(event.active()).isTrue();
		assertThat(event.version()).isEqualTo(1L);
		assertThat(event.occurredAt()).isEqualTo(Instant.parse("2023-10-01T11:00:00Z"));
		assertThat(commandStatusRecorder.eventType).isEqualTo("user.saved_coffee.set");
	}

	@Test
	void ignores_exact_command_retry_after_ack() {
		var command = new SetSavedCoffeeCommand(
				COMMAND_ID.toString(),
				SAVED_COFFEE_ID,
				USER_ID,
				COFFEE_ID,
				true,
				Instant.parse("2026-01-01T09:59:00Z"));

		handler.execute(command);
		eventPublisher.published.clear();
		handler.execute(command);

		assertThat(eventPublisher.published).isEmpty();
		assertThat(repository.allSnapshots()).hasSize(1);
	}

	private static class FakeSavedCoffeeRepository implements SavedCoffeeRepository {
		private final Map<UUID, SavedCoffee> byId = new LinkedHashMap<>();

		@Override
		public Optional<SavedCoffee> byId(UUID savedCoffeeId) {
			return Optional.ofNullable(byId.get(savedCoffeeId));
		}

		@Override
		public Optional<SavedCoffee> byUserIdAndCoffeeId(UUID userId, UUID coffeeId) {
			return byId.values().stream()
					.filter(savedCoffee -> {
						var snapshot = savedCoffee.toSnapshot();
						return snapshot.userId().equals(userId) && snapshot.coffeeId().equals(coffeeId);
					})
					.findFirst();
		}

		@Override
		public void save(SavedCoffee savedCoffee) {
			byId.put(savedCoffee.toSnapshot().savedCoffeeId(), savedCoffee);
		}

		List<SavedCoffee.SavedCoffeeSnapshot> allSnapshots() {
			return byId.values().stream().map(SavedCoffee::toSnapshot).toList();
		}
	}

	private static class RecordingCommandStatusRecorder implements CommandStatusRecorder {
		String eventType;
		private final Set<UUID> applied = new HashSet<>();

		@Override
		public void markApplied(UUID commandId, String aggregateType, String aggregateId, String eventType, Instant appliedAt) {
			this.eventType = eventType;
			applied.add(commandId);
		}

		@Override
		public boolean isApplied(UUID commandId) {
			return applied.contains(commandId);
		}
	}
}
