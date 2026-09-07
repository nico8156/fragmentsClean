package com.nm.fragmentsclean.coffeeContextTest.unit.businessLogic.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursUpdatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.DayOfWeekShort;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.UpdateCoffeeOpeningHoursCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.UpdateCoffeeOpeningHoursCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;

class UpdateCoffeeOpeningHoursCommandHandlerTest {
    private static final UUID COFFEE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void persists_a_structured_schedule_and_emits_its_versioned_fact() {
        var repository = new FakeCoffeeRepository(); var events = new FakeDomainEventPublisher();
        var clock = new DeterministicDateTimeProvider(); clock.instantOfNow = Instant.parse("2026-09-07T15:00:00Z");
        new CreateCoffeeCommandHandler(repository, events, clock).execute(createCoffee()); events.published.clear();

        new UpdateCoffeeOpeningHoursCommandHandler(repository, events, clock).execute(new UpdateCoffeeOpeningHoursCommand(
                UUID.randomUUID(), COFFEE_ID, List.of(
                        new UpdateCoffeeOpeningHoursCommand.DaySchedule(0, List.of(
                                new UpdateCoffeeOpeningHoursCommand.TimeWindow(540, 720),
                                new UpdateCoffeeOpeningHoursCommand.TimeWindow(840, 1080))),
                        new UpdateCoffeeOpeningHoursCommand.DaySchedule(1, List.of())), clock.now()));

        var coffee = repository.findById(new CoffeeId(COFFEE_ID)).orElseThrow();
        assertThat(coffee.openingHours().windowsFor(DayOfWeekShort.MONDAY)).hasSize(2);
        assertThat(coffee.openingHours().isClosed(DayOfWeekShort.TUESDAY)).isTrue();
        assertThat(coffee.version()).isEqualTo(1);
        assertThat(events.published).singleElement().isInstanceOfSatisfying(CoffeeOpeningHoursUpdatedEvent.class,
                event -> assertThat(event.periods()).hasSize(2));
    }

    @Test
    void rejects_overlapping_windows_before_persisting_anything() {
        var repository = new FakeCoffeeRepository(); var events = new FakeDomainEventPublisher();
        var clock = new DeterministicDateTimeProvider(); clock.instantOfNow = Instant.parse("2026-09-07T15:00:00Z");
        new CreateCoffeeCommandHandler(repository, events, clock).execute(createCoffee()); events.published.clear();

        var handler = new UpdateCoffeeOpeningHoursCommandHandler(repository, events, clock);
        assertThatThrownBy(() -> handler.execute(new UpdateCoffeeOpeningHoursCommand(UUID.randomUUID(), COFFEE_ID,
                List.of(new UpdateCoffeeOpeningHoursCommand.DaySchedule(0, List.of(
                        new UpdateCoffeeOpeningHoursCommand.TimeWindow(540, 720),
                        new UpdateCoffeeOpeningHoursCommand.TimeWindow(700, 900)))), clock.now())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("must not overlap");
        assertThat(events.published).isEmpty();
        assertThat(repository.findById(new CoffeeId(COFFEE_ID)).orElseThrow().version()).isZero();
    }

    private CreateCoffeeCommand createCoffee() {
        return new CreateCoffeeCommand(UUID.randomUUID(), COFFEE_ID, "place-1", "Nom", "1 rue", "Rennes", "35000", "FR",
                48.11, -1.67, null, null, List.of(), Instant.parse("2026-09-07T14:00:00Z"));
    }
}
