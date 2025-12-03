package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Coffee;

import java.util.List;
import java.util.UUID;

public record CoffeeFromGoogle(
        UUID id,
        String googleId,
        String displayName,
        String formattedAddress,
        String nationalPhoneNumber,
        String websiteUri,
        double latitude,
        double longitude,
        List<String> photoUris,
        List<String> weekdayDescriptions
) {
    public Coffee toCoffee () {
        return new Coffee(
                this.id(),
                this.googleId(),
                this.displayName(),
                this.formattedAddress(),
                this.nationalPhoneNumber(),
                this.websiteUri(),
                this.latitude(),
                this.longitude()
        );
    }
}
