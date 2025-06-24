package com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary.springboot.controllers;


import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary.abstractBaseE2E;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ReadAllCoffeesControllerIT extends abstractBaseE2E {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CoffeeRepository coffeeRepository;

    @Test
    void can_return_all_coffees() throws Exception{
        mockMvc.perform(
                get("/coffees")
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
