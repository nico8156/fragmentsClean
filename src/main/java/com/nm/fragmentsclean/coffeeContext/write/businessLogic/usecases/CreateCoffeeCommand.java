package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        Instant clientAt
) implements Command {
}
