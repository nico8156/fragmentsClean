package com.nm.fragmentsclean.sharedKernel.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.InboxMessageRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.InboxClaim;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class InboxClaimLeaseIT {
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1");
    private static JdbcTemplate jdbc;

    @BeforeAll static void database() {
        postgres.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        jdbc.execute("""
                CREATE TABLE inbox_messages (
                  id bigserial primary key, destination varchar(255) not null, event_id varchar(50) not null,
                  event_type varchar(255) not null, event_version integer not null, received_at timestamptz not null,
                  processed_at timestamptz, lease_until timestamptz, lease_owner varchar(36), status varchar(32) not null, error_message text,
                  unique(destination,event_id))
                """);
    }

    @AfterAll static void stop() { postgres.stop(); }

    @Test void active_claim_suppresses_parallel_delivery_and_failed_or_expired_claim_is_recoverable() {
        var first = new InboxMessageRepository(jdbc);
        var second = new InboxMessageRepository(jdbc);
        var event = envelope("event-lease");

        var firstClaim = first.claim(event);
        assertThat(firstClaim.acquired()).isTrue();
        assertThat(second.claim(event).status()).isEqualTo(InboxClaim.Status.BUSY);
        assertThat(first.markFailed(event, firstClaim.ownerToken(), new IllegalStateException("retry"))).isTrue();
        var secondClaim = second.claim(event);
        assertThat(secondClaim.acquired()).isTrue();
        assertThat(first.claim(event).status()).isEqualTo(InboxClaim.Status.BUSY);

        jdbc.update("UPDATE inbox_messages SET lease_until=now()-interval '1 second' WHERE event_id=?", event.eventId());
        var replacementClaim = first.claim(event);
        assertThat(replacementClaim.acquired()).isTrue();
        assertThat(second.markProcessed(event, secondClaim.ownerToken())).isFalse();
        assertThat(first.markProcessed(event, replacementClaim.ownerToken())).isTrue();
        assertThat(second.claim(event).status()).isEqualTo(InboxClaim.Status.ALREADY_PROCESSED);
    }

    private IntegrationEventEnvelope envelope(String eventId) {
        return new IntegrationEventEnvelope(eventId, "ticket.verify.accepted", 1, "source", "Ticket", "ticket-1",
                "ticket:ticket-1", "ticket-verification-requested", "{}", Instant.parse("2026-09-12T08:00:00Z"));
    }
}
