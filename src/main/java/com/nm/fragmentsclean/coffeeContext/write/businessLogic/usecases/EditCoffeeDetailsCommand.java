package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record EditCoffeeDetailsCommand(
        UUID commandId,
        UUID coffeeId,
        String name,
        String addressLine1,
        String city,
        String postalCode,
        String country,
        double latitude,
        double longitude,
        String phoneNumber,
        String website,
        Set<String> tags,
        Instant clientAt) implements Command { }
