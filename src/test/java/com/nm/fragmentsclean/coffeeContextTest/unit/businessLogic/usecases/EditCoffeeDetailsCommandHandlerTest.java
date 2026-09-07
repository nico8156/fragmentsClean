package com.nm.fragmentsclean.coffeeContextTest.unit.businessLogic.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDetailsEditedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.EditCoffeeDetailsCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.EditCoffeeDetailsCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;

class EditCoffeeDetailsCommandHandlerTest {
    private static final UUID COFFEE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void edits_the_aggregate_once_and_emits_a_versioned_fact() {
        var repository = new FakeCoffeeRepository();
        var events = new FakeDomainEventPublisher();
        var clock = new DeterministicDateTimeProvider();
        clock.instantOfNow = Instant.parse("2026-09-07T15:00:00Z");
        new CreateCoffeeCommandHandler(repository, events, clock).execute(createCommand());
        events.published.clear();

        var commandId = UUID.randomUUID();
        new EditCoffeeDetailsCommandHandler(repository, events, clock).execute(new EditCoffeeDetailsCommand(
                commandId, COFFEE_ID, "Nouveau nom", "2 rue Test", "Paris", "75001", "FR",
                48.85, 2.35, null, "https://nouveau.example", Set.of("specialty", " calme "), clock.now()));

        var coffee = repository.findById(new CoffeeId(COFFEE_ID)).orElseThrow();
        assertThat(coffee.name().value()).isEqualTo("Nouveau nom");
        assertThat(coffee.address().city()).isEqualTo("Paris");
        assertThat(coffee.phoneNumber()).isNull();
        assertThat(coffee.tags()).extracting(tag -> tag.value()).containsExactlyInAnyOrder("specialty", "calme");
        assertThat(coffee.version()).isEqualTo(1);
        assertThat(events.published).singleElement().isInstanceOfSatisfying(CoffeeDetailsEditedEvent.class, event -> {
            assertThat(event.commandId()).isEqualTo(commandId);
            assertThat(event.version()).isEqualTo(1);
        });
    }

    @Test
    void rejects_invalid_coordinates_before_persisting() {
        var repository = new FakeCoffeeRepository();
        var events = new FakeDomainEventPublisher();
        var clock = new DeterministicDateTimeProvider();
        clock.instantOfNow = Instant.parse("2026-09-07T15:00:00Z");
        new CreateCoffeeCommandHandler(repository, events, clock).execute(createCommand());
        events.published.clear();

        var handler = new EditCoffeeDetailsCommandHandler(repository, events, clock);
        assertThatThrownBy(() -> handler.execute(new EditCoffeeDetailsCommand(UUID.randomUUID(), COFFEE_ID,
                "Nom", "Rue", "Paris", "75001", "FR", 95, 2.35, null, null, Set.of(), clock.now())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("lat out of range");
        assertThat(events.published).isEmpty();
    }

    private CreateCoffeeCommand createCommand() {
        return new CreateCoffeeCommand(UUID.randomUUID(), COFFEE_ID, "place-1", "Nom", "1 rue", "Rennes",
                "35000", "FR", 48.11, -1.67, "0200000000", "https://example.com", List.of(),
                Instant.parse("2026-09-07T14:00:00Z"));
    }
}
