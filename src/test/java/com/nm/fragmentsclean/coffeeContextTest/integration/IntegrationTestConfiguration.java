package com.nm.fragmentsclean.coffeeContextTest.integration;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.SpringCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.JpaCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegrationTestConfiguration {

    @Bean
    public CoffeeRepository coffeeRepository(SpringCoffeeRepository springCoffeeRepository){
        return new JpaCoffeeRepository(springCoffeeRepository);
    }
}
