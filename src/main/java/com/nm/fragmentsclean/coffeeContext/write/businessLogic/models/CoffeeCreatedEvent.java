package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoffeeCreatedEvent(
        UUID eventId,
        UUID commandId,
        CoffeeId coffeeId,
        GooglePlaceId googlePlaceId,
        CoffeeName name,
        Address address,
        GeoPoint location,
        PhoneNumber phoneNumber,
        WebsiteUrl website,
        List<Tag> tags,
        int version,
        Instant occurredAt,
        Instant clientAt
) implements DomainEvent {

    @Override
    public UUID eventId() {
        return eventId;
    }

    @Override
    public UUID commandId() {
        return commandId;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }

    @Override
    public Instant clientAt() {
        return clientAt;
    }
}
