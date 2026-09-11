package com.nm.fragmentsclean.userApplicationContextTest.unit;

import com.nm.fragmentsclean.userApplicationContext.pass.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PassProgressPolicyTest {
    private static final UUID USER = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Test
    void first_two_levels_require_experiences_but_no_ticket() {
        assertThat(PassProgressPolicy.eligibleLevels(new PassCounters(1, 1, 0)))
                .containsExactly(PassLevel.COFFEE_TASTER);
        assertThat(PassProgressPolicy.eligibleLevels(new PassCounters(3, 3, 0)))
                .containsExactlyInAnyOrder(PassLevel.COFFEE_TASTER, PassLevel.URBAN_EXPLORER);
    }

    @Test
    void upper_levels_require_both_experiences_and_validated_tickets() {
        assertThat(PassProgressPolicy.eligibleLevels(new PassCounters(10, 5, 0)))
                .doesNotContain(PassLevel.SOCIAL_BEAN, PassLevel.FRAGMENTS_MASTER);
        assertThat(PassProgressPolicy.eligibleLevels(new PassCounters(5, 3, 1)))
                .contains(PassLevel.SOCIAL_BEAN)
                .doesNotContain(PassLevel.FRAGMENTS_MASTER);
        assertThat(PassProgressPolicy.eligibleLevels(new PassCounters(10, 5, 3)))
                .containsExactlyInAnyOrder(PassLevel.values());
    }

    @Test
    void acquired_levels_are_distinct_from_current_counters() {
        var snapshot = PassProgressPolicy.evaluate(USER, new PassCounters(0, 0, 0),
                Set.of(PassLevel.COFFEE_TASTER), 4, Instant.EPOCH);

        assertThat(snapshot.levels().get(0).status()).isEqualTo(PassLevelStatus.COMPLETED);
        assertThat(snapshot.currentLevel()).isEqualTo(PassLevel.URBAN_EXPLORER);
        assertThat(snapshot.policyVersion()).isEqualTo(2);
    }

    @Test
    void pass_owns_the_interpretation_of_experience_removal_reasons() {
        assertThat(PassProgressPolicy.countsExperience("PUBLISHED", "VISIBLE")).isTrue();
        assertThat(PassProgressPolicy.countsExperience("DRAFT", "VISIBLE")).isFalse();
        assertThat(PassProgressPolicy.countsExperience("PUBLISHED", "HIDDEN")).isFalse();
        assertThat(PassProgressPolicy.revokesAcquiredLevels(false, "AUTHOR_DELETED")).isFalse();
        assertThat(PassProgressPolicy.revokesAcquiredLevels(false, "MODERATION_HIDDEN")).isTrue();
        assertThat(PassProgressPolicy.revokesAcquiredLevels(false, "FRAUD_CONFIRMED")).isTrue();
        assertThat(PassProgressPolicy.revokesAcquiredLevels(true, "FRAUD_CONFIRMED")).isFalse();
    }
}
