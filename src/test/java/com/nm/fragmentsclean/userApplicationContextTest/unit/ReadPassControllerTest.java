package com.nm.fragmentsclean.userApplicationContextTest.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.userApplicationContext.pass.adapters.primary.ReadPassController;
import com.nm.fragmentsclean.userApplicationContext.pass.application.*;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReadPassControllerTest {
    @Test
    void returns_the_backend_owned_v2_pass_contract() {
        UUID userId = UUID.fromString("22222222-2222-4222-8222-222222222222");
        var snapshot = PassProgressPolicy.evaluate(userId, new PassCounters(5, 3, 1),
                Set.of(PassLevel.COFFEE_TASTER, PassLevel.URBAN_EXPLORER, PassLevel.SOCIAL_BEAN),
                12, Instant.parse("2026-09-11T10:00:00Z"));
        QueryBus bus = new QueryBus();
        bus.registerQueryHandlers(List.of(new GetPassQueryHandler(new FixedStore(snapshot))));

        var response = new ReadPassController(bus).get(jwt(userId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().policyVersion()).isEqualTo(2);
        assertThat(response.getBody().counters()).isEqualTo(new PassCounters(5, 3, 1));
        assertThat(response.getBody().currentLevel()).isEqualTo(PassLevel.FRAGMENTS_MASTER);
        assertThat(response.getBody().levels()).hasSize(4);
        assertThat(response.getBody().levels().get(3).requirements())
                .isEqualTo(new PassRequirements(10, 5, 3));
        assertThat(response.getBody().rights()).isEmpty();
    }

    @Test void rejects_anonymous_reads() {
        assertThat(new ReadPassController(new QueryBus()).get(null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token").header("alg", "none").subject(userId.toString()).build();
    }

    private record FixedStore(PassSnapshot snapshot) implements PassContributionStore {
        public boolean applyTicket(UUID a, UUID b, boolean c, long d, Instant e) { return false; }
        public boolean applyExperience(UUID a, UUID b, UUID c, boolean d, long e, Instant f) { return false; }
        public void lockUser(UUID userId) { }
        public PassCounters counters(UUID userId) { return snapshot.counters(); }
        public Optional<PassSnapshot> find(UUID userId) { return Optional.of(snapshot); }
        public void save(PassSnapshot snapshot) { }
    }
}
