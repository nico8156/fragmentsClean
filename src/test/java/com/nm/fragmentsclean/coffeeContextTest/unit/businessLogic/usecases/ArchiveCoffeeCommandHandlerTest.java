package com.nm.fragmentsclean.coffeeContextTest.unit.businessLogic.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ArchiveCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ArchiveCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;

class ArchiveCoffeeCommandHandlerTest {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID COFFEE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	FakeCoffeeRepository coffeeRepository = new FakeCoffeeRepository();
	FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();
	DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();
	CreateCoffeeCommandHandler createHandler;
	ArchiveCoffeeCommandHandler archiveHandler;

	@BeforeEach
	void setUp() {
		dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T11:00:00Z");
		createHandler = new CreateCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
		archiveHandler = new ArchiveCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
	}

	@Test
	void archives_existing_coffee_without_physical_delete_and_publishes_event() {
		createHandler.execute(createCommand());
		domainEventPublisher.published.clear();

		archiveHandler.execute(new ArchiveCoffeeCommand(
				COMMAND_ID,
				COFFEE_ID,
				Instant.parse("2026-07-04T10:59:59Z")
		));

		assertThat(coffeeRepository.allSnapshots()).hasSize(1);
		assertThat(coffeeRepository.allSnapshots().getFirst().archivedAt()).isEqualTo(dateTimeProvider.instantOfNow);
		assertThat(domainEventPublisher.published).hasSize(1);
		var event = (CoffeeArchivedEvent) domainEventPublisher.published.getFirst();
		assertThat(event.commandId()).isEqualTo(COMMAND_ID);
		assertThat(event.coffeeId().value()).isEqualTo(COFFEE_ID);
		assertThat(event.version()).isEqualTo(1);
		assertThat(event.occurredAt()).isEqualTo(dateTimeProvider.instantOfNow);
		assertThat(event.clientAt()).isEqualTo(Instant.parse("2026-07-04T10:59:59Z"));
	}

	@Test
	void ignores_missing_coffee_without_publishing_event() {
		archiveHandler.execute(new ArchiveCoffeeCommand(
				COMMAND_ID,
				COFFEE_ID,
				Instant.parse("2026-07-04T10:59:59Z")
		));

		assertThat(coffeeRepository.allSnapshots()).isEmpty();
		assertThat(domainEventPublisher.published).isEmpty();
	}

	@Test
	void ignores_already_archived_coffee_without_publishing_second_event() {
		createHandler.execute(createCommand());
		domainEventPublisher.published.clear();
		archiveHandler.execute(new ArchiveCoffeeCommand(COMMAND_ID, COFFEE_ID, Instant.parse("2026-07-04T10:59:59Z")));
		domainEventPublisher.published.clear();

		archiveHandler.execute(new ArchiveCoffeeCommand(
				UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
				COFFEE_ID,
				Instant.parse("2026-07-04T11:00:59Z")
		));

		assertThat(coffeeRepository.allSnapshots()).hasSize(1);
		assertThat(domainEventPublisher.published).isEmpty();
	}

	private CreateCoffeeCommand createCommand() {
		return new CreateCoffeeCommand(
				UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
				COFFEE_ID,
				"google-place-1",
				"Fragments Cafe",
				"1 rue Example",
				"Rennes",
				"35000",
				"FR",
				48.11,
				-1.67,
				"0200000000",
				"https://example.com",
				List.of("google-places"),
				Instant.parse("2026-07-04T10:55:00Z")
		);
	}
}
