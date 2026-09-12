package com.nm.fragmentsclean.ticketContext.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationJob;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketVerificationJobTest {
    private static final Instant NOW = Instant.parse("2026-09-12T08:00:00Z");

    @Test
    void claim_retry_reclaim_and_complete_are_explicit_transitions() {
        var job = requested();

        job.claim("worker-a", NOW, Duration.ofSeconds(30));
        assertThat(job.snapshot().state()).isEqualTo(TicketVerificationJob.State.RUNNING);
        assertThat(job.snapshot().attempts()).isEqualTo(1);

        job.retry("worker-a", "timeout", NOW.plusSeconds(1), NOW.plusSeconds(6));
        assertThat(job.snapshot().state()).isEqualTo(TicketVerificationJob.State.RETRY_PENDING);
        assertThat(job.claimableAt(NOW.plusSeconds(5))).isFalse();

        job.claim("worker-b", NOW.plusSeconds(6), Duration.ofSeconds(30));
        job.complete("worker-b", NOW.plusSeconds(7));
        assertThat(job.snapshot().state()).isEqualTo(TicketVerificationJob.State.COMPLETED);
        assertThat(job.snapshot().leaseOwner()).isNull();
    }

    @Test
    void active_lease_and_stale_worker_are_rejected() {
        var job = requested();
        job.claim("worker-a", NOW, Duration.ofSeconds(30));

        assertThatThrownBy(() -> job.claim("worker-b", NOW.plusSeconds(10), Duration.ofSeconds(30)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("active");
        assertThatThrownBy(() -> job.complete("worker-b", NOW.plusSeconds(11)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("lease owner");
    }

    @Test
    void expired_running_job_can_be_reclaimed() {
        var job = requested();
        job.claim("worker-a", NOW, Duration.ofSeconds(5));

        job.claim("worker-b", NOW.plusSeconds(5), Duration.ofSeconds(30));

        assertThat(job.snapshot().leaseOwner()).isEqualTo("worker-b");
        assertThat(job.snapshot().attempts()).isEqualTo(2);
    }

    private TicketVerificationJob requested() {
        return TicketVerificationJob.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TOTAL 4,00 EUR", "s3://ticket", NOW.minusSeconds(10), NOW);
    }
}
