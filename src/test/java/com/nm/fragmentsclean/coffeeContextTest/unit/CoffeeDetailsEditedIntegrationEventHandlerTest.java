package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.CoffeeDetailsEditedIntegrationEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeDetailsEditedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;

class CoffeeDetailsEditedIntegrationEventHandlerTest {
    @Test
    void refreshes_from_the_authoritative_source_and_notifies_only_after_a_newer_public_snapshot() {
        var id = UUID.randomUUID();
        var view = new CoffeeSummaryView(id, "place", "Nom", 48, 2, "Rue", "Paris", "75000", "FR",
                null, null, Set.of(), "PUBLISHED", 3, Instant.parse("2026-09-07T15:00:00Z"));
        var repository = new RecordingRepository();
        var signals = new ArrayList<ProjectionSyncEvent>();
        var handler = new CoffeeDetailsEditedIntegrationEventHandler(coffeeId -> Optional.of(view), repository, signals::add);

        handler.handle(new CoffeeDetailsEditedIntegrationEvent(UUID.randomUUID(), UUID.randomUUID(), id, 3,
                view.updatedAt(), view.updatedAt()));
        handler.handle(new CoffeeDetailsEditedIntegrationEvent(UUID.randomUUID(), UUID.randomUUID(), id, 2,
                view.updatedAt(), view.updatedAt()));

        assertThat(repository.views).containsExactly(view);
        assertThat(signals).hasSize(1);
    }

    private static final class RecordingRepository implements CoffeeProjectionRepository {
        private final List<CoffeeSummaryView> views = new ArrayList<>();
        @Override public boolean isPublished(UUID coffeeId) { return true; }
        @Override public void apply(CoffeeCreatedEvent event) { }
        @Override public void apply(CoffeeSummaryView view) { views.add(view); }
        @Override public CoffeeProjectionMutation applyIfNewer(CoffeeSummaryView view) {
            if (!views.isEmpty()) return CoffeeProjectionMutation.ignored(views.getFirst().version(), views.getFirst().updatedAt());
            views.add(view); return CoffeeProjectionMutation.applied(view.version(), view.updatedAt());
        }
        @Override public void deleteByCoffeeId(UUID coffeeId) { }
        @Override public List<CoffeeSummaryView> findAll() { return List.of(); }
        @Override public void insertSeed(CoffeeSummaryView view) { }
        @Override public long count() { return 0; }
    }
}
