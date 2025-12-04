package com.nm.fragmentsclean.coffeeContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
public class CoffeeProjectionIT extends AbstractBaseE2E {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COFFEE_ID  = UUID.fromString("07dae867-1273-4d0f-b1dd-f206b290626b");

    private static final String GOOGLE_PLACE_ID = "ChIJB8tVJh3eDkgRrbxiSh2Jj3c";

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
    void creating_coffee_populates_coffee_projection_via_outbox_and_eventbus() throws Exception {
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
                                          "name": "Columbus Café & Co",
                                          "addressLine1": "Centre Commercial Grand Quartier",
                                          "city": "Saint-Grégoire",
                                          "postalCode": "35760",
                                          "country": "FR",
                                          "lat": 48.1368282,
                                          "lon": -1.6953883,
                                          "phoneNumber": "02 99 54 25 82",
                                          "website": "https://www.columbuscafe.com/boutique/saint-gregoire-centre-commercial-grand-quartier/",
                                          "tags": ["espresso", "chain"],
                                          "at": "%s"
                                        }
                                        """.formatted(
                                                COMMAND_ID,
                                                COFFEE_ID,
                                                GOOGLE_PLACE_ID,
                                                clientAt
                                        )
                                )
                )
                .andExpect(status().isAccepted());

        // WHEN : on déclenche manuellement le dispatcher outbox → eventBus → projection
        outboxEventDispatcher.dispatchPending();

        // THEN : la projection a bien été alimentée
        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList("SELECT * FROM coffee_summaries_projection");

        assertThat(rows).hasSize(1);
        var row = rows.getFirst();

        assertThat(row.get("id")).isEqualTo(COFFEE_ID);
        assertThat(row.get("google_place_id")).isEqualTo(GOOGLE_PLACE_ID);
        assertThat(row.get("name")).isEqualTo("Columbus Café & Co");
        assertThat(row.get("address_line1")).isEqualTo("Centre Commercial Grand Quartier");
        assertThat(row.get("city")).isEqualTo("Saint-Grégoire");
        assertThat(row.get("postal_code")).isEqualTo("35760");
        assertThat(row.get("country")).isEqualTo("FR");
        assertThat(row.get("lat")).isEqualTo(48.1368282);
        assertThat(row.get("lon")).isEqualTo(-1.6953883);
        assertThat(row.get("phone_number")).isEqualTo("02 99 54 25 82");
        assertThat(row.get("website")).isEqualTo("https://www.columbuscafe.com/boutique/saint-gregoire-centre-commercial-grand-quartier/");
        // tags_json : tu peux vérifier aussi si tu veux (via cast String -> contains("espresso"))
        assertThat(row.get("version")).isEqualTo(0); // première version
        // updated_at / rating : libre à toi de les vérifier aussi
    }
}
