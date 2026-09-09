package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialPublicationSchedule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EditorialPublicationScheduleTest {
    private static final Instant NOW = Instant.parse("2026-09-08T10:00:00Z");
    private static final UUID ARTICLE = UUID.randomUUID();
    private static final UUID REVISION = UUID.randomUUID();

    @Test void publication_requires_a_revision_and_a_non_past_due_date() {
        assertThatThrownBy(() -> EditorialPublicationSchedule.schedule(UUID.randomUUID(), ARTICLE, null,
                EditorialPublicationSchedule.Operation.PUBLISH, NOW.plusSeconds(1), NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EditorialPublicationSchedule.schedule(UUID.randomUUID(), ARTICLE, REVISION,
                EditorialPublicationSchedule.Operation.PUBLISH, NOW.minusSeconds(1), NOW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void protects_lease_ownership_and_allows_expired_lease_recovery() {
        var schedule = EditorialPublicationSchedule.schedule(UUID.randomUUID(), ARTICLE, REVISION,
                EditorialPublicationSchedule.Operation.PUBLISH, NOW, NOW);
        schedule.claim("worker-a", NOW, NOW.plusSeconds(30));

        assertThatThrownBy(() -> schedule.markDispatched("worker-b", NOW.plusSeconds(1))).isInstanceOf(IllegalStateException.class);
        schedule.claim("worker-b", NOW.plusSeconds(30), NOW.plusSeconds(60));
        schedule.markDispatched("worker-b", NOW.plusSeconds(31));
        schedule.complete();

        assertThat(schedule.snapshot().status()).isEqualTo(EditorialPublicationSchedule.Status.COMPLETED);
        assertThat(schedule.snapshot().version()).isEqualTo(4);
    }

    @Test void only_a_scheduled_intent_can_be_cancelled() {
        var schedule = EditorialPublicationSchedule.schedule(UUID.randomUUID(), ARTICLE, null,
                EditorialPublicationSchedule.Operation.ARCHIVE, NOW.plusSeconds(10), NOW);
        schedule.cancel();
        assertThat(schedule.snapshot().status()).isEqualTo(EditorialPublicationSchedule.Status.CANCELLED);
        assertThatThrownBy(schedule::cancel).isInstanceOf(IllegalStateException.class);
    }
}
