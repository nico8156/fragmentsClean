package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers.CoffeeFromGoogle;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers.GooglePlacesApi;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;

import java.util.List;

public class CreateCoffeeCommandHandler implements CommandHandler<CreateCoffeeCommand> {
    private final CoffeeRepository coffeeRepository;
    private final GooglePlacesApi googlePlacesApi;

    public CreateCoffeeCommandHandler(CoffeeRepository coffeeRepository, GooglePlacesApi googlePlacesApi) {
        this.coffeeRepository = coffeeRepository;
        this.googlePlacesApi = googlePlacesApi;
    }
    public void execute(CreateCoffeeCommand command) {
        List<CoffeeFromGoogle> coffeeList = googlePlacesApi.searchCoffeesNearby(command.latitude(), command.longitude());
        coffeeList.forEach(c -> coffeeRepository.save(c.toCoffee()));
    }
}
