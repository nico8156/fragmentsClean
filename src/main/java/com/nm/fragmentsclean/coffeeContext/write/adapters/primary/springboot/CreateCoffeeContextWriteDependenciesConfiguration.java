package com.nm.fragmentsclean.coffeeContext.write.adapters.primary.springboot;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.providers.FakeGooglePlacesApi;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.SpringCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.JpaCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers.GooglePlacesApi;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories(basePackages = "com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa")
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
    public CoffeeRepository coffeeRepository(SpringCoffeeRepository springCoffeeRepository){
        return new JpaCoffeeRepository(springCoffeeRepository);
    }
}
