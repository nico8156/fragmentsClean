package com.nm.fragmentsclean.coffeeContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.coffeeContext.write.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/coffees")
public class WriteCoffeeController {
    private final CommandBus commandBus;

    public WriteCoffeeController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    record CreateCoffeeRequest(double latitude, double longitude) {}

    @PostMapping
    public ResponseEntity<Void> createCoffee(
            @RequestBody CreateCoffeeRequest createCoffeeRequest) {
        commandBus.dispatch(new CreateCoffeeCommand(createCoffeeRequest.latitude(), createCoffeeRequest.longitude()));
        return ResponseEntity.created(URI.create("/coffees")).build();
    }
}
