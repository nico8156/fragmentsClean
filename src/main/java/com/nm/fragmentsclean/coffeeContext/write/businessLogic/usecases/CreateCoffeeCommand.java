package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePublicationStatus;

public record CreateCoffeeCommand(
        UUID commandId,
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
        List<String> tags,
        Instant clientAt,
        CoffeePublicationStatus publicationStatus
) implements Command {
    public CreateCoffeeCommand(UUID commandId, UUID coffeeId, String googlePlaceId, String name,
                               String addressLine1, String city, String postalCode, String country,
                               double lat, double lon, String phoneNumber, String website,
                               List<String> tags, Instant clientAt) {
        this(commandId, coffeeId, googlePlaceId, name, addressLine1, city, postalCode, country,
                lat, lon, phoneNumber, website, tags, clientAt, CoffeePublicationStatus.PUBLISHED);
    }
}
