package com.nm.fragmentsclean.coffeeContextTest.unit.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateCoffeeCommandHandlerTest {

    private static final UUID CMD_ID    = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COFFEE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final String GOOGLE_PLACE_ID = "ChIJB8tVJh3eDkgRrbxiSh2Jj3c";

    FakeCoffeeRepository coffeeRepository = new FakeCoffeeRepository();
    FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();
    DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();

    CreateCoffeeCommandHandler handler;

    @BeforeEach
    void setup() {
        dateTimeProvider.instantOfNow = Instant.parse("2024-01-01T10:00:00Z");
        handler = new CreateCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
    }

    @Test
    void should_create_new_coffee_and_publish_event() {
        // GIVEN
        var clientAt = Instant.parse("2024-01-01T09:59:00Z");

        var cmd = new CreateCoffeeCommand(
                CMD_ID,
                COFFEE_ID,
                GOOGLE_PLACE_ID,
                "Columbus Café & Co",
                "Centre Commercial Grand Quartier",
                "Saint-Grégoire",
                "35760",
                "FR",
                48.1368282,
                -1.6953883,
                "02 99 54 25 82",
                "https://www.columbuscafe.com/boutique/saint-gregoire-centre-commercial-grand-quartier/",
                List.of("espresso", "chain"),
                clientAt
        );

        // WHEN
        handler.execute(cmd);

        // THEN : état en repo
        var snaps = coffeeRepository.allSnapshots();
        assertThat(snaps).hasSize(1);
        var snap = snaps.getFirst();

        assertThat(snap.coffeeId()).isEqualTo(COFFEE_ID);
        assertThat(snap.googlePlaceId()).isEqualTo(GOOGLE_PLACE_ID);
        assertThat(snap.name()).isEqualTo("Columbus Café & Co");
        assertThat(snap.addressLine1()).isEqualTo("Centre Commercial Grand Quartier");
        assertThat(snap.city()).isEqualTo("Saint-Grégoire");
        assertThat(snap.postalCode()).isEqualTo("35760");
        assertThat(snap.country()).isEqualTo("FR");
        assertThat(snap.lat()).isEqualTo(48.1368282);
        assertThat(snap.lon()).isEqualTo(-1.6953883);
        assertThat(snap.phoneNumber()).isEqualTo("02 99 54 25 82");
        assertThat(snap.website()).isEqualTo("https://www.columbuscafe.com/boutique/saint-gregoire-centre-commercial-grand-quartier/");
        assertThat(snap.tags()).containsExactlyInAnyOrder("espresso", "chain");
        assertThat(snap.version()).isEqualTo(0);
        assertThat(snap.updatedAt()).isEqualTo(dateTimeProvider.instantOfNow);

        // THEN : event publié
        assertThat(domainEventPublisher.published).hasSize(1);
        var evt = (CoffeeCreatedEvent) domainEventPublisher.published.getFirst();

        assertThat(evt.commandId()).isEqualTo(CMD_ID);
        assertThat(evt.coffeeId().value()).isEqualTo(COFFEE_ID);
        assertThat(evt.googlePlaceId().value()).isEqualTo(GOOGLE_PLACE_ID);
        assertThat(evt.name().value()).isEqualTo("Columbus Café & Co");

        assertThat(evt.address().line1()).isEqualTo("Centre Commercial Grand Quartier");
        assertThat(evt.address().city()).isEqualTo("Saint-Grégoire");
        assertThat(evt.address().postalCode()).isEqualTo("35760");
        assertThat(evt.address().country()).isEqualTo("FR");

        assertThat(evt.location().lat()).isEqualTo(48.1368282);
        assertThat(evt.location().lon()).isEqualTo(-1.6953883);

        assertThat(evt.phoneNumber().value()).isEqualTo("02 99 54 25 82");
        assertThat(evt.website().value()).isEqualTo("https://www.columbuscafe.com/boutique/saint-gregoire-centre-commercial-grand-quartier/");

        assertThat(evt.tags()).extracting(t -> t.value())
                .containsExactlyInAnyOrder("espresso", "chain");

        assertThat(evt.version()).isEqualTo(0);
        assertThat(evt.occurredAt()).isEqualTo(dateTimeProvider.instantOfNow);
        assertThat(evt.clientAt()).isEqualTo(clientAt);
    }
}
