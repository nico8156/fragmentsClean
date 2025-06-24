package com.nm.fragmentsclean.coffeeContext.read;

import java.util.UUID;

public record CoffeeView(
        UUID id,
        String googleId,
        String displayName,
        String formattedAddress,
        String nationalPhoneNumber,
        String websiteUri,
        double latitude,
        double longitude
) {
}
