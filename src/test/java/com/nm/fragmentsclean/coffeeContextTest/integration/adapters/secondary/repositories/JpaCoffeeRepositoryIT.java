package com.nm.fragmentsclean.coffeeContextTest.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.SpringCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeeJpaEntity;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;

import com.nm.fragmentsclean.coffeeContextTest.integration.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaCoffeeRepositoryIT extends AbstractJpaIntegrationTest {

    private static final UUID COFFEE_ID = UUID.fromString("07dae867-1273-4d0f-b1dd-f206b290626b");
    private static final String GOOGLE_PLACE_ID = "ChIJB8tVJh3eDkgRrbxiSh2Jj3c";

    @Autowired
    private CoffeeRepository coffeeRepository;

    @Autowired
    private SpringCoffeeRepository springCoffeeRepository;


    @Test
    void repositories_are_injected() {
        assertThat(coffeeRepository).isNotNull();
        assertThat(springCoffeeRepository).isNotNull();
    }

    @Test
    void can_save_a_coffee() {
        var now = Instant.parse("2024-01-01T10:00:00Z");

        var coffeeId = new CoffeeId(COFFEE_ID);
        var googleId = new GooglePlaceId(GOOGLE_PLACE_ID);
        var name     = new CoffeeName("Columbus Café & Co");
        var address  = new Address(
                "Centre Commercial Grand Quartier",
                "Saint-Grégoire",
                "35760",
                "FR"
        );
        var location = new GeoPoint(48.1368282, -1.6953883);
        var phone    = new PhoneNumber("02 99 54 25 82");
        var website  = new WebsiteUrl("https://www.columbuscafe.com/boutique/saint-gregoire-centre-commercial-grand-quartier/");
        var tags     = Set.of(new Tag("espresso"), new Tag("chain"));

        Coffee coffee = Coffee.createNew(
                coffeeId,
                googleId,
                name,
                address,
                location,
                phone,
                website,
                tags,
                now
        );

        // WHEN
        coffeeRepository.save(coffee);

        // THEN : on vérifie la ligne JPA brute
        var entityOpt = springCoffeeRepository.findById(COFFEE_ID);
        assertThat(entityOpt).isPresent();
        CoffeeJpaEntity entity = entityOpt.get();

        assertThat(entity.getId()).isEqualTo(COFFEE_ID);
        assertThat(entity.getGooglePlaceId()).isEqualTo(GOOGLE_PLACE_ID);
        assertThat(entity.getName()).isEqualTo("Columbus Café & Co");
        assertThat(entity.getAddressLine1()).isEqualTo("Centre Commercial Grand Quartier");
        assertThat(entity.getCity()).isEqualTo("Saint-Grégoire");
        assertThat(entity.getPostalCode()).isEqualTo("35760");
        assertThat(entity.getCountry()).isEqualTo("FR");
        assertThat(entity.getLat()).isEqualTo(48.1368282);
        assertThat(entity.getLon()).isEqualTo(-1.6953883);
        assertThat(entity.getPhoneNumber()).isEqualTo("02 99 54 25 82");
        assertThat(entity.getWebsite()).isEqualTo("https://www.columbuscafe.com/boutique/saint-gregoire-centre-commercial-grand-quartier/");
        assertThat(entity.getTagsCsv()).contains("espresso", "chain");
        assertThat(entity.getVersion()).isEqualTo(0);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void can_update_a_coffee() {
        var createdAt = Instant.parse("2024-01-01T10:00:00Z");

        // GIVEN : on persiste une première version
        var coffee = Coffee.createNew(
                new CoffeeId(COFFEE_ID),
                new GooglePlaceId(GOOGLE_PLACE_ID),
                new CoffeeName("Old Name"),
                new Address("Old address", "Old city", "00000", "FR"),
                new GeoPoint(48.0, -1.0),
                new PhoneNumber("0000000000"),
                new WebsiteUrl("https://old.example.com"),
                Set.of(new Tag("old")),
                createdAt
        );
        coffeeRepository.save(coffee);

        // WHEN : on recharge côté domaine, on change quelques champs, on re-save
        var loaded = coffeeRepository.findById(new CoffeeId(COFFEE_ID)).orElseThrow();

        var editedAt = Instant.parse("2024-01-01T11:00:00Z");
        loaded.rename(new CoffeeName("New Name"), editedAt);
        loaded.changeContact(
                new PhoneNumber("01 23 45 67 89"),
                new WebsiteUrl("https://new.example.com"),
                editedAt
        );
        coffeeRepository.save(loaded);

        // THEN : côté JPA brut
        var entity = springCoffeeRepository.findById(COFFEE_ID).orElseThrow();
        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getPhoneNumber()).isEqualTo("01 23 45 67 89");
        assertThat(entity.getWebsite()).isEqualTo("https://new.example.com");
        assertThat(entity.getVersion()).isEqualTo(2);              // version++ via touch()
        assertThat(entity.getUpdatedAt()).isEqualTo(editedAt);     // mis à jour
    }
}
