package com.nm.fragmentsclean.coffeeContext.read.configuration;


import com.nm.fragmentsclean.coffeeContext.read.ListCoffeesQueryHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.JpaCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.SpringCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories("com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class CoffeeContextDependenciesConfiguration {

    @Bean
    @Profile("database")
    public CoffeeRepository jpaCoffeeRepository(SpringCoffeeRepository springCoffeeRepository){
        return new JpaCoffeeRepository(springCoffeeRepository);
    }

    @Bean
    CreateCoffeeCommandHandler createCoffeeCommandHandler(CoffeeRepository coffeeRepository,
                                                          DomainEventPublisher domainEventPublisher,
                                                          DateTimeProvider dateTimeProvider){
        return new CreateCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
    }

    @Bean
    ListCoffeesQueryHandler listCoffeesQueryHandler(CoffeeProjectionRepository coffeeProjectionRepository){
        return new ListCoffeesQueryHandler(coffeeProjectionRepository);
    }
}
