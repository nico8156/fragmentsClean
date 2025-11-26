package com.nm.fragmentsclean.coffeeContextTest.integration;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.SpringCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.JpaCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@TestConfiguration
public class IntegrationTestConfiguration {

    @Bean
    public CoffeeRepository coffeeRepository(SpringCoffeeRepository springCoffeeRepository){
        return new JpaCoffeeRepository(springCoffeeRepository);
    }
}
