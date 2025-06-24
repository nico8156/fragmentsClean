package com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers.CoffeeFromGoogle;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;
import com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary.abstractBaseE2E;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WriteCoffeeControllerIT extends abstractBaseE2E {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CoffeeRepository coffeeRepository;

    @BeforeEach
    void setUp() {
        coffeeRepository.deleteAll();
    }


    @Test
    void can_write_coffee_when_given_valid_coordinates() throws Exception{
        mockMvc.perform(
                post("/coffees")
                        .contentType("application/json")
                        .content(
                                """
                                {
                                  "latitude": 141.14,
                                  "longitude": 12.54
                                }
                                """
                        )
        )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/coffees"));

        List<Coffee> coffeeList = coffeeRepository.findAll();
        assertThat(coffeeList.size()).isEqualTo(2);
        assertThat(coffeeList).contains(new Coffee(
                UUID.fromString("DB87BC49-1357-4512-A4F5-B0422C57222F"),
                "fhsqmjkfh",
                "un super café",
                "24 rue de rennes 35000 Rennes",
                "0612345678",
                "http://www.unsupercafe.com",
                132.96,
                96.45
        ));
    }
}
