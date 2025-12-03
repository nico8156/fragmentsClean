package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes;


import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Tag;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;



public class FakeCoffeeRepository implements CoffeeRepository {

    public record CoffeeSnapshot(
            UUID coffeeId,
            String googlePlaceId,
            String name,
            String addressLine1,
            String city,
            String postalCode,
            String country,
            double lat,
            double lon,
            String phoneNumber,
            String website,
            Set<String> tags,
            int version,
            Instant updatedAt
    ) {}

    private final Map<CoffeeId, Coffee> store = new HashMap<>();
    private final List<CoffeeSnapshot> snapshots = new ArrayList<>();

    @Override
    public void save(Coffee coffee) {
        store.put(coffee.coffeeId(), coffee);

        snapshots.clear();
        snapshots.addAll(
                store.values().stream()
                        .map(this::toSnapshot)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public Optional<Coffee> findById(CoffeeId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public boolean existsByGooglePlaceId(GooglePlaceId googlePlaceId) {
        return store.values().stream()
                .anyMatch(c ->
                        c.googleId().isPresent()
                                && c.googleId().get().value().equals(googlePlaceId.value())
                );
    }

    private CoffeeSnapshot toSnapshot(Coffee c) {
        var addr = c.address();
        var loc = c.location();

        return new CoffeeSnapshot(
                c.coffeeId().value(),
                c.googleId().map(GooglePlaceId::value).orElse(null),
                c.name().value(),
                addr.line1(),
                addr.city(),
                addr.postalCode(),
                addr.country(),
                loc.lat(),
                loc.lon(),
                c.phoneNumber() != null ? c.phoneNumber().value() : null,
                c.website() != null ? c.website().value() : null,
                c.tags().stream().map(Tag::value).collect(Collectors.toSet()),
                c.version(),
                c.updatedAt()
        );
    }

    public List<CoffeeSnapshot> allSnapshots() {
        return List.copyOf(snapshots);
    }
}
