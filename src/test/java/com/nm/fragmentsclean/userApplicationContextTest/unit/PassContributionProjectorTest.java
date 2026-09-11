package com.nm.fragmentsclean.userApplicationContextTest.unit;

import com.nm.fragmentsclean.userApplicationContext.pass.application.PassContributionProjector;
import com.nm.fragmentsclean.userApplicationContext.pass.application.PassContributionStore;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PassContributionProjectorTest {
    private static final UUID USER = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");

    @Test
    void ordinary_deletion_reduces_counters_but_preserves_an_acquired_level() {
        var store = new FakeStore();
        var projector = new PassContributionProjector(store);
        store.counters = new PassCounters(1, 1, 0);
        var acquired = projector.experienceChanged(UUID.randomUUID(), USER, UUID.randomUUID(),
                true, false, 1, NOW).orElseThrow();
        assertThat(acquired.acquiredLevels()).containsExactly(PassLevel.COFFEE_TASTER);

        store.counters = new PassCounters(0, 0, 0);
        var afterDelete = projector.experienceChanged(UUID.randomUUID(), USER, UUID.randomUUID(),
                false, false, 2, NOW.plusSeconds(1)).orElseThrow();

        assertThat(afterDelete.counters()).isEqualTo(new PassCounters(0, 0, 0));
        assertThat(afterDelete.acquiredLevels()).containsExactly(PassLevel.COFFEE_TASTER);
    }

    @Test
    void moderation_or_fraud_can_revoke_levels_no_longer_supported_by_contributions() {
        var store = new FakeStore();
        store.snapshot = Optional.of(PassProgressPolicy.evaluate(USER, new PassCounters(1, 1, 0),
                Set.of(PassLevel.COFFEE_TASTER), 3, NOW));
        store.counters = new PassCounters(0, 0, 0);

        var result = new PassContributionProjector(store).experienceChanged(
                UUID.randomUUID(), USER, UUID.randomUUID(), false, true, 4, NOW.plusSeconds(1));

        assertThat(result.orElseThrow().acquiredLevels()).isEmpty();
    }

    @Test
    void stale_source_versions_are_no_ops() {
        var store = new FakeStore();
        store.apply = false;
        assertThat(new PassContributionProjector(store).ticketChanged(
                UUID.randomUUID(), USER, true, false, 1, NOW)).isEmpty();
        assertThat(store.saved).isZero();
    }

    private static final class FakeStore implements PassContributionStore {
        boolean apply = true;
        int saved;
        PassCounters counters = new PassCounters(0, 0, 0);
        Optional<PassSnapshot> snapshot = Optional.empty();
        public boolean applyTicket(UUID a, UUID b, boolean c, long d, Instant e) { return apply; }
        public boolean applyExperience(UUID a, UUID b, UUID c, boolean d, long e, Instant f) { return apply; }
        public void lockUser(UUID userId) { }
        public PassCounters counters(UUID userId) { return counters; }
        public Optional<PassSnapshot> find(UUID userId) { return snapshot; }
        public void save(PassSnapshot value) { snapshot = Optional.of(value); saved++; }
    }
}
