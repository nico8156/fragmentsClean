package com.nm.fragmentsclean.sharedKernel.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.CommandStatusRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.JdbcOutboxDeliveryStore;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxRetryPolicy;
import java.time.Duration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.containers.PostgreSQLContainer;

class JdbcOutboxDeliveryStoreIT {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:13.1");
    private static final Instant NOW = Instant.parse("2026-09-17T10:00:00Z");
    private static JdbcTemplate jdbc;
    private JdbcOutboxDeliveryStore first;
    private JdbcOutboxDeliveryStore second;

    @BeforeAll static void database() {
        POSTGRES.start();
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE outbox_events (
                  id bigserial primary key, event_id varchar(50) not null unique, event_type varchar(255) not null,
                  aggregate_type varchar(100) not null, aggregate_id varchar(100) not null,
                  stream_key varchar(255) not null, payload_json text not null,
                  occurred_at timestamptz not null, created_at timestamptz not null,
                  status varchar(32) not null, retry_count integer not null default 0,
                  next_attempt_at timestamptz, lease_until timestamptz,
                  lease_owner varchar(128), last_error varchar(1000))
                """);
        jdbc.execute("""
                CREATE TABLE command_status (
                  command_id uuid primary key, requester_id uuid, command_type varchar(255), fingerprint varchar(64),
                  status varchar(32) not null, aggregate_type varchar(100), aggregate_id varchar(100),
                  event_id varchar(50), event_type varchar(255), applied_at timestamptz, rejected_at timestamptz,
                  rejection_code varchar(100), reason text, updated_at timestamptz not null)
                """);
        var statuses = new CommandStatusRepository(jdbc, new ObjectMapper().findAndRegisterModules());
        var transactions = new DataSourceTransactionManager(dataSource);
        // Both adapters share the database but have independent claim calls, like two application instances.
        HOLDER.first = new JdbcOutboxDeliveryStore(jdbc, statuses, transactions);
        HOLDER.second = new JdbcOutboxDeliveryStore(jdbc, statuses, transactions);
    }

    private static final class HOLDER {
        private static JdbcOutboxDeliveryStore first;
        private static JdbcOutboxDeliveryStore second;
    }

    @AfterAll static void stop() { POSTGRES.stop(); }

    @BeforeEach void reset() {
        jdbc.update("DELETE FROM command_status");
        jdbc.update("DELETE FROM outbox_events");
        first = HOLDER.first;
        second = HOLDER.second;
    }

    @Test void active_lease_prevents_double_claim_and_expiry_allows_recovery() {
        insert("event-1", "stream-1", UUID.randomUUID());

        var claim = first.claimDue("worker-1", NOW, NOW.plusSeconds(30), 10);

        assertThat(claim).hasSize(1);
        assertThat(second.claimDue("worker-2", NOW.plusSeconds(1), NOW.plusSeconds(31), 10)).isEmpty();
        assertThat(second.claimDue("worker-2", NOW.plusSeconds(31), NOW.plusSeconds(61), 10))
                .extracting(event -> event.eventId()).containsExactly("event-1");
        assertThat(first.markSent(claim.getFirst(), "worker-1", NOW.plusSeconds(32))).isFalse();
    }

    @Test void stream_order_is_preserved_while_other_streams_can_progress() {
        insert("event-1", "stream-a", UUID.randomUUID());
        insert("event-2", "stream-a", UUID.randomUUID());
        insert("event-3", "stream-b", UUID.randomUUID());

        var claimed = first.claimDue("worker-1", NOW, NOW.plusSeconds(30), 10);

        assertThat(claimed).extracting(event -> event.eventId()).containsExactly("event-1", "event-3");
        assertThat(second.claimDue("worker-2", NOW, NOW.plusSeconds(30), 10)).isEmpty();
    }

    @Test void successful_completion_atomically_updates_outbox_and_command_status() {
        UUID commandId = UUID.randomUUID();
        insert("event-1", "stream-1", commandId);
        var message = first.claimDue("worker-1", NOW, NOW.plusSeconds(30), 1).getFirst();

        assertThat(first.markSent(message, "worker-1", NOW.plusSeconds(1))).isTrue();

        assertThat(jdbc.queryForObject("SELECT status FROM outbox_events WHERE event_id='event-1'", String.class))
                .isEqualTo("SENT");
        assertThat(jdbc.queryForObject("SELECT status FROM command_status WHERE command_id=?", String.class, commandId))
                .isEqualTo("APPLIED");
    }

    @Test void failed_delivery_is_not_claimable_before_its_backoff_expires() {
        insert("event-1", "stream-1", UUID.randomUUID());
        var message = first.claimDue("worker-1", NOW, NOW.plusSeconds(30), 1).getFirst();

        assertThat(first.recordFailure(message.id(), "worker-1", 1, NOW.plusSeconds(20), "SQS unavailable", false))
                .isTrue();

        assertThat(second.claimDue("worker-2", NOW.plusSeconds(19), NOW.plusSeconds(49), 1)).isEmpty();
        assertThat(second.claimDue("worker-2", NOW.plusSeconds(20), NOW.plusSeconds(50), 1)).hasSize(1);
    }

    @Test void claim_reads_legacy_hibernate_large_object_payload_without_mutating_it() {
        UUID commandId = UUID.randomUUID();
        insert("event-1", "stream-1", commandId);
        jdbc.update("""
                UPDATE outbox_events
                SET payload_json = lo_from_bytea(0, convert_to(payload_json, 'UTF8'))::text
                WHERE event_id = 'event-1'
                """);

        var claimed = first.claimDue("worker-1", NOW, NOW.plusSeconds(30), 1);

        assertThat(claimed).singleElement().extracting(message -> message.payloadJson())
                .isEqualTo("{\"commandId\":\"" + commandId + "\"}");
        assertThat(jdbc.queryForObject(
                "SELECT payload_json ~ '^[0-9]+$' FROM outbox_events WHERE event_id='event-1'", Boolean.class))
                .isTrue();
    }

    @Test void dispatcher_never_holds_a_database_transaction_while_calling_the_sender() {
        insert("event-1", "stream-1", UUID.randomUUID());
        var transactionObserved = new boolean[]{true};
        var dispatcher = new OutboxEventDispatcher(first,
                event -> transactionObserved[0] = TransactionSynchronizationManager.isActualTransactionActive(),
                () -> NOW, new OutboxRetryPolicy(3, Duration.ofSeconds(1), Duration.ofMinutes(1)),
                Duration.ofSeconds(30), 1, "worker-1");

        dispatcher.dispatchPending();

        assertThat(transactionObserved[0]).isFalse();
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_events WHERE event_id='event-1'", String.class))
                .isEqualTo("SENT");
    }

    @Test void dispatcher_drains_one_stream_in_order_within_its_bounded_batch() {
        insert("event-1", "stream-1", UUID.randomUUID());
        insert("event-2", "stream-1", UUID.randomUUID());
        var delivered = new ArrayList<String>();
        var dispatcher = new OutboxEventDispatcher(first, event -> delivered.add(event.eventId()),
                () -> NOW, new OutboxRetryPolicy(3, Duration.ofSeconds(1), Duration.ofMinutes(1)),
                Duration.ofSeconds(30), 2, "worker-1");

        dispatcher.dispatchPending();

        assertThat(delivered).containsExactly("event-1", "event-2");
    }

    private void insert(String eventId, String streamKey, UUID commandId) {
        jdbc.update("""
                INSERT INTO outbox_events(event_id,event_type,aggregate_type,aggregate_id,stream_key,payload_json,
                  occurred_at,created_at,status,retry_count)
                VALUES (?,?,?,?,?,?,?,?,'PENDING',0)
                """, eventId, "example.Event", "Example", "aggregate-1", streamKey,
                "{\"commandId\":\"" + commandId + "\"}", Timestamp.from(NOW), Timestamp.from(NOW));
    }
}
