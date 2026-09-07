package com.nm.fragmentsclean.coffeeContext.read;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.DayOfWeekShort;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeOpeningHoursUpdatedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

public class CoffeeOpeningHoursUpdatedIntegrationEventHandler {
    private final CoffeeOpeningHoursProjectionRepository projection;
    private final CoffeePublicProjectionChangePolicy publicChangePolicy;
    private final ProjectionSyncPublisher sync;

    public CoffeeOpeningHoursUpdatedIntegrationEventHandler(CoffeeOpeningHoursProjectionRepository projection,
            CoffeeProjectionRepository coffees, ProjectionSyncPublisher sync) {
        this.projection = projection;
        this.publicChangePolicy = new CoffeePublicProjectionChangePolicy(coffees);
        this.sync = sync;
    }

    @Transactional
    public void handle(CoffeeOpeningHoursUpdatedIntegrationEvent event) {
        projection.replaceForCoffee(event.coffeeId(), views(event));
        if (!publicChangePolicy.isPubliclyVisible(event.coffeeId())) return;
        sync.publish(ProjectionSyncEvent.projectionUpdated("coffees", "entity", event.coffeeId().toString(),
                (long) event.version(), event.occurredAt(), List.of("openingHours")));
    }

    private List<CoffeeOpeningHoursView> views(CoffeeOpeningHoursUpdatedIntegrationEvent event) {
        var byDay = event.periods().stream().collect(Collectors.groupingBy(CoffeeOpeningHoursUpdatedIntegrationEvent.OpeningPeriod::dayCode));
        return java.util.Arrays.stream(DayOfWeekShort.values()).map(day -> {
            var periods = byDay.getOrDefault(day.code(), List.of()).stream()
                    .sorted(Comparator.comparingInt(CoffeeOpeningHoursUpdatedIntegrationEvent.OpeningPeriod::startMinute)).toList();
            var description = label(day) + ": " + (periods.isEmpty() ? "Fermé" : periods.stream()
                    .map(period -> format(period.startMinute()) + "–" + format(period.endMinute()))
                    .collect(Collectors.joining(", ")));
            return new CoffeeOpeningHoursView(UUID.nameUUIDFromBytes((event.coffeeId() + ":opening-hours:" + day.code())
                    .getBytes(StandardCharsets.UTF_8)), event.coffeeId(), description);
        }).toList();
    }

    private String label(DayOfWeekShort day) {
        return switch (day) { case MONDAY -> "Lundi"; case TUESDAY -> "Mardi"; case WEDNESDAY -> "Mercredi";
            case THURSDAY -> "Jeudi"; case FRIDAY -> "Vendredi"; case SATURDAY -> "Samedi"; case SUNDAY -> "Dimanche"; };
    }
    private String format(int minutes) { return String.format("%02d:%02d", minutes / 60, minutes % 60); }
}
