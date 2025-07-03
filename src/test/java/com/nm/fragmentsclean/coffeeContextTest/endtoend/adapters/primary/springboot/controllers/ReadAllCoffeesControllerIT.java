package com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.SpringCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeeJpaEntity;
import com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary.abstractBaseE2E;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ReadAllCoffeesControllerIT extends abstractBaseE2E {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringCoffeeRepository springCoffeeRepository;

    @BeforeEach
    void setUp() {
        springCoffeeRepository.deleteAll();
        springCoffeeRepository.save(new CoffeeJpaEntity(
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
    @Test
    void can_return_all_coffees() throws Exception{
        mockMvc.perform(
                get("/coffees")
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        assertThat(springCoffeeRepository.findAll().size()).isEqualTo(1);
    }
}
