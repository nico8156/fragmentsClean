package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.CoffeeOpeningHoursUpdatedIntegrationEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import com.nm.fragmentsclean.coffeeContextTest.support.PublishedCoffeeProjectionRepository;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeOpeningHoursUpdatedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

class CoffeeOpeningHoursUpdatedIntegrationEventHandlerTest {
    @Test
    void replaces_projection_with_every_day_and_notifies_public_catalogue() {
        var repository = new RecordingRepository(); var sync = new RecordingSync();
        var event = new CoffeeOpeningHoursUpdatedIntegrationEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), List.of(
                new CoffeeOpeningHoursUpdatedIntegrationEvent.OpeningPeriod(0, 540, 720),
                new CoffeeOpeningHoursUpdatedIntegrationEvent.OpeningPeriod(0, 840, 1080)), 3,
                Instant.parse("2026-09-07T15:00:00Z"), null);
        new CoffeeOpeningHoursUpdatedIntegrationEventHandler(repository, new PublishedCoffeeProjectionRepository(), sync).handle(event);
        assertThat(repository.views).hasSize(7);
        assertThat(repository.views.getFirst().weekdayDescription()).isEqualTo("Lundi: 09:00–12:00, 14:00–18:00");
        assertThat(repository.views.get(1).weekdayDescription()).isEqualTo("Mardi: Fermé");
        assertThat(sync.events).singleElement().satisfies(result -> assertThat(result.hints()).containsExactly("openingHours"));
    }

    private static final class RecordingRepository implements CoffeeOpeningHoursProjectionRepository {
        private List<CoffeeOpeningHoursView> views = List.of();
        public void insertSeed(CoffeeOpeningHoursView value) { }
        public void replaceForCoffee(UUID id, List<CoffeeOpeningHoursView> values) { views = List.copyOf(values); }
        public void deleteForCoffee(UUID id) { }
        public List<CoffeeOpeningHoursView> findAll() { return views; }
        public long count() { return views.size(); }
    }
    private static final class RecordingSync implements ProjectionSyncPublisher {
        private final List<ProjectionSyncEvent> events = new ArrayList<>();
        public void publish(ProjectionSyncEvent event) { events.add(event); }
    }
}
