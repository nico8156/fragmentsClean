package com.nm.fragmentsclean.ticketContext.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers.AbstractBaseE2E;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusProjectionRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminDeletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminUpdatedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketProjectionOrderIT extends AbstractBaseE2E {
    private static final UUID USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");

    @Autowired JdbcTicketStatusProjectionRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clearProjection() {
        jdbc.update("DELETE FROM ticket_status_projection");
    }

    @Test
    void stale_and_duplicate_events_cannot_overwrite_a_completed_projection() {
        UUID ticketId = UUID.randomUUID();
        var accepted = accepted(ticketId, 0L);
        var completed = approved(ticketId, 1L);

        assertThat(repository.applyAnalyzing(accepted)).isTrue();
        assertThat(repository.applyCompleted(completed)).isTrue();
        assertThat(repository.applyAnalyzing(accepted)).isFalse();
        assertThat(repository.applyCompleted(completed)).isFalse();

        assertThat(jdbc.queryForObject(
                "SELECT status FROM ticket_status_projection WHERE ticket_id=?",
                String.class, ticketId)).isEqualTo("CONFIRMED");
    }

    @Test
    void a_delete_tombstone_applied_first_blocks_older_events() {
        UUID ticketId = UUID.randomUUID();
        var deleted = new TicketAdminDeletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), ticketId, USER_ID,
                UUID.randomUUID(), 3L, NOW.plusSeconds(3));

        assertThat(repository.applyAdminDeleted(deleted)).isTrue();
        assertThat(repository.applyAnalyzing(accepted(ticketId, 0L))).isFalse();
        assertThat(repository.applyCompleted(approved(ticketId, 1L))).isFalse();
        assertThat(repository.applyAdminDeleted(deleted)).isFalse();

        var row = jdbc.queryForMap(
                "SELECT status, version, merchant_name FROM ticket_status_projection WHERE ticket_id=?",
                ticketId);
        assertThat(row).containsEntry("status", "DELETED").containsEntry("version", 3L);
        assertThat(row.get("merchant_name")).isNull();
    }

    @Test
    void an_admin_update_can_build_a_missing_projection_and_blocks_older_events() {
        UUID ticketId = UUID.randomUUID();
        var updated = new TicketAdminUpdatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), ticketId, USER_ID,
                "CONFIRMED", "corrected OCR", "private-image", 700, "EUR", NOW,
                "Corrected Cafe", "Paris", "CARD", null,
                2L, UUID.randomUUID(), NOW.plusSeconds(2));

        assertThat(repository.applyAdminUpdated(updated)).isTrue();
        assertThat(repository.applyCompleted(approved(ticketId, 1L))).isFalse();
        assertThat(repository.applyAdminUpdated(updated)).isFalse();

        var row = jdbc.queryForMap(
                "SELECT status, version, merchant_name FROM ticket_status_projection WHERE ticket_id=?",
                ticketId);
        assertThat(row).containsEntry("status", "CONFIRMED")
                .containsEntry("version", 2L)
                .containsEntry("merchant_name", "Corrected Cafe");
    }

    private TicketVerifyAcceptedEvent accepted(UUID ticketId, long version) {
        return new TicketVerifyAcceptedEvent(
                UUID.randomUUID(), UUID.randomUUID(), ticketId, USER_ID,
                "OCR", "private-image", Ticket.TicketStatus.ANALYZING.name(),
                version, NOW, NOW);
    }

    private TicketVerificationCompletedEvent approved(UUID ticketId, long version) {
        return new TicketVerificationCompletedEvent(
                UUID.randomUUID(), UUID.randomUUID(), ticketId, USER_ID,
                TicketVerificationCompletedEvent.Outcome.APPROVED, version,
                NOW.plusSeconds(1), NOW,
                new TicketVerificationCompletedEvent.Approved(
                        450, "EUR", NOW, "Cafe", "Paris", "CARD", List.of()),
                null, "ticketEngine", "trace");
    }
}
