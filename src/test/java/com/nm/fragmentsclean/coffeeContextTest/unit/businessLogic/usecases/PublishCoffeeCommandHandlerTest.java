package com.nm.fragmentsclean.coffeeContextTest.unit.businessLogic.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePublicationStatus;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePublishedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.PublishCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.PublishCoffeeCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PublishCoffeeCommandHandlerTest {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COFFEE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private final FakeCoffeeRepository repository = new FakeCoffeeRepository();
    private final FakeDomainEventPublisher events = new FakeDomainEventPublisher();
    private final DeterministicDateTimeProvider clock = new DeterministicDateTimeProvider();
    private PublishCoffeeCommandHandler handler;

    @BeforeEach
    void setUp() {
        clock.instantOfNow = Instant.parse("2026-08-30T12:00:00Z");
        new CreateCoffeeCommandHandler(repository, events, clock).execute(createCommand());
        var created = repository.findById(new CoffeeId(COFFEE_ID)).orElseThrow();
        repository.save(com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee.rehydrate(
                created.coffeeId(), created.googleId().orElse(null), created.name(), created.address(),
                created.location(), created.phoneNumber(), created.website(), created.tags(), created.photos(),
                created.openingHours(), created.version(), created.updatedAt(), created.archivedAt().orElse(null),
                CoffeePublicationStatus.DRAFT));
        events.published.clear();
        handler = new PublishCoffeeCommandHandler(repository, events, clock);
    }

    @Test
    void publishes_draft_and_emits_versioned_fact() {
        var clientAt = Instant.parse("2026-08-30T11:59:00Z");

        handler.execute(new PublishCoffeeCommand(COMMAND_ID, COFFEE_ID, clientAt));

        assertThat(repository.findById(new CoffeeId(COFFEE_ID)).orElseThrow().publicationStatus())
                .isEqualTo(CoffeePublicationStatus.PUBLISHED);
        assertThat(events.published).hasSize(1);
        var event = (CoffeePublishedEvent) events.published.getFirst();
        assertThat(event.commandId()).isEqualTo(COMMAND_ID);
        assertThat(event.coffeeId().value()).isEqualTo(COFFEE_ID);
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.occurredAt()).isEqualTo(clock.now());
        assertThat(event.clientAt()).isEqualTo(clientAt);
    }

    @Test
    void is_idempotent_when_coffee_is_already_published() {
        handler.execute(new PublishCoffeeCommand(COMMAND_ID, COFFEE_ID, clock.instantOfNow));
        events.published.clear();

        handler.execute(new PublishCoffeeCommand(UUID.randomUUID(), COFFEE_ID, clock.instantOfNow));

        assertThat(events.published).isEmpty();
        assertThat(repository.findById(new CoffeeId(COFFEE_ID)).orElseThrow().version()).isEqualTo(1);
    }

    private CreateCoffeeCommand createCommand() {
        return new CreateCoffeeCommand(
                UUID.randomUUID(), COFFEE_ID, "google-place-1", "Fragments Cafe",
                "1 rue Example", "Rennes", "35000", "FR", 48.11, -1.67,
                "0200000000", "https://example.com", List.of("google-places"),
                Instant.parse("2026-08-30T11:55:00Z"));
    }
}
