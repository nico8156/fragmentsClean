package com.nm.fragmentsclean.coffeeContext.write.configuration;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.providers.FakeGooglePlacesApi;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers.GooglePlacesApi;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CreateCoffeeContextWriteDependenciesConfiguration {
    @Bean
    public CreateCoffeeCommandHandler createCoffeeCommandHandler(
            CoffeeRepository coffeeRepository,
            GooglePlacesApi googlePlacesApi
    ){
        return new CreateCoffeeCommandHandler(coffeeRepository, googlePlacesApi);
    }

    @Bean
    public GooglePlacesApi googlePlacesApi(){
        return new FakeGooglePlacesApi();
    }
    @Bean
    public CoffeeRepository coffeeRepository(){
        return new FakeCoffeeRepository();
    }
}
