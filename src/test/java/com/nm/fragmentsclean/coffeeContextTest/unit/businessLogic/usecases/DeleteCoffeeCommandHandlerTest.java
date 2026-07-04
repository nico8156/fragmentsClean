package com.nm.fragmentsclean.coffeeContextTest.unit.businessLogic.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.DeleteCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.DeleteCoffeeCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;

class DeleteCoffeeCommandHandlerTest {

	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID COFFEE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	FakeCoffeeRepository coffeeRepository = new FakeCoffeeRepository();
	FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();
	DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();
	CreateCoffeeCommandHandler createHandler;
	DeleteCoffeeCommandHandler deleteHandler;

	@BeforeEach
	void setUp() {
		dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T11:00:00Z");
		createHandler = new CreateCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
		deleteHandler = new DeleteCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
	}

	@Test
	void deletes_existing_coffee_and_publishes_event() {
		createHandler.execute(createCommand());
		domainEventPublisher.published.clear();

		deleteHandler.execute(new DeleteCoffeeCommand(
				COMMAND_ID,
				COFFEE_ID,
				Instant.parse("2026-07-04T10:59:59Z")
		));

		assertThat(coffeeRepository.allSnapshots()).isEmpty();
		assertThat(domainEventPublisher.published).hasSize(1);
		var event = (CoffeeDeletedEvent) domainEventPublisher.published.getFirst();
		assertThat(event.commandId()).isEqualTo(COMMAND_ID);
		assertThat(event.coffeeId().value()).isEqualTo(COFFEE_ID);
		assertThat(event.version()).isEqualTo(1);
		assertThat(event.occurredAt()).isEqualTo(dateTimeProvider.instantOfNow);
		assertThat(event.clientAt()).isEqualTo(Instant.parse("2026-07-04T10:59:59Z"));
	}

	@Test
	void ignores_missing_coffee_without_publishing_event() {
		deleteHandler.execute(new DeleteCoffeeCommand(
				COMMAND_ID,
				COFFEE_ID,
				Instant.parse("2026-07-04T10:59:59Z")
		));

		assertThat(coffeeRepository.allSnapshots()).isEmpty();
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
