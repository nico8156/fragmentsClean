package com.nm.fragmentsclean.coffeeContextTest.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.SpringCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeeJpaEntity;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Coffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Coffee.CoffeeSnapshot;
import com.nm.fragmentsclean.coffeeContextTest.integration.AbstractTestIntegration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaCoffeeRepositoryIT extends AbstractTestIntegration {

    private static final UUID COFFEE_ID = UUID.fromString("DB87BC49-1357-4512-A4F5-B0422C57222F");

    @Autowired
    private CoffeeRepository coffeeRepository;

    @Autowired
    private SpringCoffeeRepository springCoffeeRepository;

    @Test
    void can_save_a_coffee(){
        var coffeeSnapshot = new CoffeeSnapshot(
                COFFEE_ID,
                "fnsqlmik=",
                "uncafeparfait",
                "24 rue de rennes 35000 Rennes",
                "0612345678",
                "http://www.unsupercafe.com",
                132.96,
                96.45
        );
        coffeeRepository.save(Coffee.fromSnapShot(coffeeSnapshot));

        assertThat(springCoffeeRepository.findAll()).contains(
                new CoffeeJpaEntity(
                        COFFEE_ID,
                        "fnsqlmik=",
                        "uncafeparfait",
                        "24 rue de rennes 35000 Rennes",
                        "0612345678",
                        "http://www.unsupercafe.com",
                        132.96,
                        96.45
                )
        );

    }

}
