package com.nm.fragmentsclean.sharedKernel.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxRetryPolicy;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxRetryPolicyTest {
    private final OutboxRetryPolicy policy =
            new OutboxRetryPolicy(4, Duration.ofSeconds(1), Duration.ofSeconds(5));

    @Test void exponential_delay_is_bounded_and_deterministic() {
        assertThat(policy.delayFor("event-1", 1)).isEqualTo(policy.delayFor("event-1", 1));
        assertThat(policy.delayFor("event-1", 1)).isBetween(Duration.ofMillis(800), Duration.ofMillis(1_200));
        assertThat(policy.delayFor("event-1", 20)).isBetween(Duration.ofSeconds(4), Duration.ofSeconds(6));
    }

    @Test void only_the_configured_failure_count_is_terminal() {
        assertThat(policy.terminal(3)).isFalse();
        assertThat(policy.terminal(4)).isTrue();
    }
}
