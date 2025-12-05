package com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
public class CoffeeReadIT extends AbstractBaseE2E {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COFFEE_ID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String GOOGLE_ID = "ChIJ-Coffee-Google-Place-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringOutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventDispatcher outboxEventDispatcher;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM coffee_summaries_projection");
        outboxEventRepository.deleteAll();
    }

    @Test
    void creating_coffee_then_listing_from_projection() throws Exception {
        // GIVEN
        var clientAt = "2024-02-14T08:00:00Z";

        mockMvc.perform(
                        post("/api/coffees")
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "commandId": "%s",
                                          "coffeeId": "%s",
                                          "googlePlaceId": "%s",
                                          "name": "Fragments Café",
                                          "latitude": 48.111,
                                          "longitude": -1.680,
                                          "addressLine": "1 Rue des Fragments",
                                          "city": "Rennes",
                                          "postalCode": "35000",
                                          "country": "FR",
                                          "phoneNumber": "0102030405",
                                          "website": "https://fragments.example",
                                          "tags": ["specialty", "filter"],
                                          "at": "%s"
                                        }
                                        """.formatted(
                                                COMMAND_ID,
                                                COFFEE_ID,
                                                GOOGLE_ID,
                                                clientAt
                                        )
                                )
                )
                .andExpect(status().isAccepted());

        // WHEN : on déclenche la chaîne outbox → eventBus → projection
        outboxEventDispatcher.dispatchPending();

        // THEN : GET /api/coffees
        var mvcResult = mockMvc.perform(
                        get("/api/coffees")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(COFFEE_ID.toString()))
                .andExpect(jsonPath("$[0].googleId").value(GOOGLE_ID))
                .andExpect(jsonPath("$[0].name").value("Fragments Café"))
                .andExpect(jsonPath("$[0].city").value("Rennes"))
                .andReturn();

        // Optionnel : sanity check en base sur la projection
        var rows = jdbcTemplate.queryForList("SELECT * FROM coffee_summaries_projection");
        assertThat(rows).hasSize(1);
        var row = rows.get(0);
        assertThat(row.get("id").toString()).isEqualTo(COFFEE_ID.toString());
        assertThat(row.get("name")).isEqualTo("Fragments Café");
    }
}
