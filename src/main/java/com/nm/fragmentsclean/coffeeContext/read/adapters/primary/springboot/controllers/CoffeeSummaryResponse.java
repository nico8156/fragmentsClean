package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;


import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CoffeeSummaryResponse(
        UUID id,
        String googleId,
        String name,
        double latitude,
        double longitude,
        String addressLine,
        String city,
        String postalCode,
        String country,
        String phoneNumber,
        String website,
        Set<String> tags,
        long version,
        Instant updatedAt
) {
    public static CoffeeSummaryResponse from(CoffeeSummaryView v) {
        return new CoffeeSummaryResponse(
                v.id(),
                v.googleId(),
                v.name(),
                v.latitude(),
                v.longitude(),
                v.addressLine(),
                v.city(),
                v.postalCode(),
                v.country(),
                v.phoneNumber(),
                v.website(),
                v.tags(),
                v.version(),
                v.updatedAt()
        );
    }
}
