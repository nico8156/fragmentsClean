package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers.CoffeeSummaryResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminCoffeeResponseContractTest {

    @Test
    void exposes_projection_publication_status_to_studio() {
        var summary = new CoffeeSummaryResponse(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "google-place-1",
                "Fragments Cafe",
                new CoffeeSummaryResponse.Location(48.11, -1.67),
                new CoffeeSummaryResponse.Address("1 rue Example", "Rennes", "35000", "FR"),
                null,
                null,
                Set.of("coffee"),
                "DRAFT",
                0,
                Instant.parse("2026-08-30T14:32:50Z"));

        var response = AdminCoffeesReadController.AdminCoffeeResponse.from(summary, List.of(), List.of());

        assertThat(response.publicationStatus()).isEqualTo("DRAFT");
    }
}
