package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.JdbcCoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CoffeeCreatedProjectionHandler implements EventHandler<CoffeeCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(CoffeeCreatedProjectionHandler.class);

    private final JdbcCoffeeProjectionRepository projectionRepository;

    public CoffeeCreatedProjectionHandler(JdbcCoffeeProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }


    @Override
    public void handle(CoffeeCreatedEvent event) {
        log.info("Applying CoffeeCreatedEvent to projection for coffeeId={}", event.coffeeId().value());
        projectionRepository.apply(event);
    }
}

