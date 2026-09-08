package com.nm.fragmentsclean.coffeeContextTest.unit.businessLogic.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePublicationStatus;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeUnpublishedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ArchiveCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ArchiveCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.UnpublishCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.UnpublishCoffeeCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnpublishCoffeeCommandHandlerTest {
    private static final UUID COFFEE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final FakeCoffeeRepository repository = new FakeCoffeeRepository();
    private final FakeDomainEventPublisher events = new FakeDomainEventPublisher();
    private final DeterministicDateTimeProvider clock = new DeterministicDateTimeProvider();
    private UnpublishCoffeeCommandHandler handler;

    @BeforeEach
    void setUp() {
        clock.instantOfNow = Instant.parse("2026-09-08T08:00:00Z");
        new CreateCoffeeCommandHandler(repository, events, clock).execute(new CreateCoffeeCommand(
                UUID.randomUUID(), COFFEE_ID, "google-place-1", "Fragments Cafe", "1 rue Example", "Rennes", "35000", "FR",
                48.11, -1.67, "0200000000", "https://example.com", List.of("google-places"), clock.now()));
        events.published.clear();
        handler = new UnpublishCoffeeCommandHandler(repository, events, clock);
    }

    @Test
    void unpublishes_a_published_coffee_and_emits_a_versioned_fact() {
        var commandId = UUID.randomUUID();
        handler.execute(new UnpublishCoffeeCommand(commandId, COFFEE_ID, Instant.parse("2026-09-08T07:59:00Z")));

        var coffee = repository.findById(new CoffeeId(COFFEE_ID)).orElseThrow();
        assertThat(coffee.publicationStatus()).isEqualTo(CoffeePublicationStatus.DRAFT);
        assertThat(events.published).singleElement().isInstanceOf(CoffeeUnpublishedEvent.class);
        var event = (CoffeeUnpublishedEvent) events.published.getFirst();
        assertThat(event.commandId()).isEqualTo(commandId);
        assertThat(event.coffeeId().value()).isEqualTo(COFFEE_ID);
        assertThat(event.version()).isEqualTo(1);
    }

    @Test
    void is_idempotent_when_coffee_is_already_a_draft() {
        handler.execute(new UnpublishCoffeeCommand(UUID.randomUUID(), COFFEE_ID, clock.now()));
        events.published.clear();

        handler.execute(new UnpublishCoffeeCommand(UUID.randomUUID(), COFFEE_ID, clock.now()));

        assertThat(events.published).isEmpty();
        assertThat(repository.findById(new CoffeeId(COFFEE_ID)).orElseThrow().version()).isEqualTo(1);
    }

    @Test
    void rejects_unpublishing_an_archived_coffee() {
        new ArchiveCoffeeCommandHandler(repository, events, clock)
                .execute(new ArchiveCoffeeCommand(UUID.randomUUID(), COFFEE_ID, clock.now()));

        assertThatThrownBy(() -> handler.execute(new UnpublishCoffeeCommand(UUID.randomUUID(), COFFEE_ID, clock.now())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Archived coffee cannot be unpublished");
    }
}
