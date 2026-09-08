package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialAuthorityLevel;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSource;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSourceAccessMode;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSourceStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EditorialSourceTest {
    private static final UUID SOURCE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant NOW = Instant.parse("2026-09-08T10:00:00Z");

    @Test
    void schedules_a_successful_consultation_from_the_source_own_cadence() {
        var source = source();

        source.claimConsultation("worker-a", NOW, NOW.plusSeconds(60));
        source.completeConsultation(3, "etag-42", "item-42", NOW.minusSeconds(60), NOW);

        var snapshot = source.snapshot();
        assertThat(snapshot.status()).isEqualTo(EditorialSourceStatus.HEALTHY);
        assertThat(snapshot.lastSuccessfulCheckAt()).isEqualTo(NOW);
        assertThat(snapshot.nextCheckAt()).isEqualTo(NOW.plus(Duration.ofHours(6)));
        assertThat(snapshot.checkpoint().etag()).isEqualTo("etag-42");
        assertThat(snapshot.checkpoint().lastExternalId()).isEqualTo("item-42");
        assertThat(snapshot.leaseOwner()).isNull();
    }

    @Test
    void cannot_be_claimed_twice_before_its_lease_expires() {
        var source = source();
        source.claimConsultation("worker-a", NOW, NOW.plusSeconds(60));

        assertThatThrownBy(() -> source.claimConsultation("worker-b", NOW.plusSeconds(20), NOW.plusSeconds(80)))
                .hasMessageContaining("lease is still active");
    }

    @Test
    void schedules_source_local_backoff_after_failure_without_disabling_the_source() {
        var source = source();
        source.claimConsultation("worker-a", NOW, NOW.plusSeconds(60));

        source.failConsultation("HTTP_TIMEOUT", NOW);

        var snapshot = source.snapshot();
        assertThat(snapshot.status()).isEqualTo(EditorialSourceStatus.DEGRADED);
        assertThat(snapshot.enabled()).isTrue();
        assertThat(snapshot.nextCheckAt()).isEqualTo(NOW.plus(Duration.ofHours(1)));
        assertThat(snapshot.failureCount()).isEqualTo(1);
    }

    @Test
    void rejects_completion_from_a_stale_worker() {
        var source = source();
        source.claimConsultation("worker-a", NOW, NOW.plusSeconds(60));

        assertThatThrownBy(() -> source.completeConsultation("worker-b", 1, null, null, null, NOW))
                .hasMessageContaining("lease owner");
    }

    private EditorialSource source() {
        return EditorialSource.register(
                SOURCE_ID,
                "Perfect Daily Grind",
                EditorialSourceAccessMode.RSS,
                EditorialAuthorityLevel.SPECIALIZED_MEDIA,
                "https://perfectdailygrind.com/feed/",
                Duration.ofHours(6),
                NOW);
    }
}
