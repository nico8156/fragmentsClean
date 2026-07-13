package com.nm.fragmentsclean.ticketContext.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.ticketContext.read.pass.PassCountersView;
import com.nm.fragmentsclean.ticketContext.read.pass.PassLevel;
import com.nm.fragmentsclean.ticketContext.read.pass.PassLevelStatus;
import com.nm.fragmentsclean.ticketContext.read.pass.PassProgressPolicy;
import org.junit.jupiter.api.Test;

class PassProgressPolicyTest {

    @Test
    void starts_with_coffee_taster_in_progress() {
        var counters = new PassCountersView(0, 0, 0);

        var levels = PassProgressPolicy.levelsFor(counters);

        assertThat(PassProgressPolicy.currentLevel(counters)).isEqualTo(PassLevel.COFFEE_TASTER);
        assertThat(levels.get(0).status()).isEqualTo(PassLevelStatus.IN_PROGRESS);
        assertThat(levels.get(1).status()).isEqualTo(PassLevelStatus.LOCKED);
    }

    @Test
    void unlocks_urban_after_coffee_taster_requirement() {
        var counters = new PassCountersView(3, 0, 0);

        var levels = PassProgressPolicy.levelsFor(counters);

        assertThat(levels.get(0).status()).isEqualTo(PassLevelStatus.COMPLETED);
        assertThat(levels.get(1).status()).isEqualTo(PassLevelStatus.IN_PROGRESS);
        assertThat(PassProgressPolicy.currentLevel(counters)).isEqualTo(PassLevel.URBAN_EXPLORER);
    }

    @Test
    void completes_social_bean_and_master_without_extra_master_threshold() {
        var counters = new PassCountersView(10, 5, 5);

        var levels = PassProgressPolicy.levelsFor(counters);

        assertThat(levels.get(2).status()).isEqualTo(PassLevelStatus.COMPLETED);
        assertThat(levels.get(3).level()).isEqualTo(PassLevel.FRAGMENTS_MASTER);
        assertThat(levels.get(3).status()).isEqualTo(PassLevelStatus.COMPLETED);
        assertThat(levels.get(3).requirements().validatedTickets()).isNull();
        assertThat(levels.get(3).requirements().publishedComments()).isNull();
        assertThat(levels.get(3).requirements().confirmedLikes()).isNull();
        assertThat(PassProgressPolicy.currentLevel(counters)).isEqualTo(PassLevel.FRAGMENTS_MASTER);
    }

    @Test
    void publishes_rights_from_completed_pass_levels() {
        assertThat(PassProgressPolicy.rightsFor(new PassCountersView(5, 3, 0))).containsExactly("COMMENT");
        assertThat(PassProgressPolicy.rightsFor(new PassCountersView(10, 5, 5))).containsExactly("COMMENT", "LIKE");
    }
}
