package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeeJpaEntity;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;


import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import java.util.stream.Collectors;

public class JpaCoffeeRepository implements CoffeeRepository {

    private final SpringCoffeeRepository springRepo;

    public JpaCoffeeRepository(SpringCoffeeRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(Coffee coffee) {
        CoffeeJpaEntity entity = mapToEntity(coffee);
        springRepo.save(entity);
    }

    @Override
    public Optional<Coffee> findById(CoffeeId id) {
        return springRepo.findById(id.value())
                .map(this::mapToDomain);
    }

    @Override
    public boolean existsByGooglePlaceId(GooglePlaceId googlePlaceId) {
        return springRepo.existsByGooglePlaceId(googlePlaceId.value());
    }

    // ========= Mapping domain -> JPA =========

    private CoffeeJpaEntity mapToEntity(Coffee c) {
        String tagsCsv = c.tags().isEmpty()
                ? null
                : c.tags().stream()
                .map(Tag::value)
                .collect(Collectors.joining(","));

        var addr = c.address();
        var loc = c.location();
        String googlePlaceId = c.googleId().map(GooglePlaceId::value).orElse(null);

        return new CoffeeJpaEntity(
                c.coffeeId().value(),
                googlePlaceId,
                c.name().value(),
                addr.line1(),
                addr.city(),
                addr.postalCode(),
                addr.country(),
                loc.lat(),
                loc.lon(),
                c.phoneNumber() != null ? c.phoneNumber().value() : null,
                c.website() != null ? c.website().value() : null,
                tagsCsv,
                c.version(),
                c.updatedAt() != null ? c.updatedAt() : Instant.now()
        );
    }

    // ========= Mapping JPA -> domain =========

    private Coffee mapToDomain(CoffeeJpaEntity e) {
        CoffeeId id = new CoffeeId(e.getId());

        GooglePlaceId googlePlaceId = e.getGooglePlaceId() != null
                ? new GooglePlaceId(e.getGooglePlaceId())
                : null;

        CoffeeName name = new CoffeeName(e.getName());

        Address address = new Address(
                e.getAddressLine1(),
                e.getCity(),
                e.getPostalCode(),
                e.getCountry()
        );

        GeoPoint location = new GeoPoint(e.getLat(), e.getLon());

        PhoneNumber phone = e.getPhoneNumber() != null
                ? new PhoneNumber(e.getPhoneNumber())
                : null;

        WebsiteUrl website = e.getWebsite() != null
                ? new WebsiteUrl(e.getWebsite())
                : null;

        Set<Tag> tags = e.getTagsCsv() == null || e.getTagsCsv().isBlank()
                ? Set.of()
                : Arrays.stream(e.getTagsCsv().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Tag::new)
                .collect(Collectors.toUnmodifiableSet());

        // on ne reconstruit pas encore photos / openingHours : ce sera une étape suivante
        return Coffee.rehydrate(
                id,
                googlePlaceId,
                name,
                address,
                location,
                phone,
                website,
                tags,
                /* photos */ null,
                /* openingHours */ null,
                e.getVersion(),
                e.getUpdatedAt()
        );
    }
}
