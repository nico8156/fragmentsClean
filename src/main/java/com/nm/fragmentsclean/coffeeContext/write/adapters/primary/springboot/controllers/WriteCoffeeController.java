package com.nm.fragmentsclean.coffeeContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/coffees")
public class WriteCoffeeController {

    private final CommandBus commandBus;

    public WriteCoffeeController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    public record CreateCoffeeRequestDto(
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
            Instant at // clientAt
    ) { }

    @PostMapping
    public ResponseEntity<Void> createCoffee(@RequestBody CreateCoffeeRequestDto dto) {
        var command = new CreateCoffeeCommand(
                dto.commandId(),
                dto.coffeeId(),
                dto.googlePlaceId(),
                dto.name(),
                dto.addressLine1(),
                dto.city(),
                dto.postalCode(),
                dto.country(),
                dto.lat(),
                dto.lon(),
                dto.phoneNumber(),
                dto.website(),
                dto.tags(),
                dto.at()
        );

        commandBus.dispatch(command);

        // Même convention que pour /api/articles : on répond 202
        return ResponseEntity.accepted().build();
    }
}
