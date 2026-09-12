package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class TicketVerificationHealthIndicatorIT {
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1");
    private static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void database() {
        postgres.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        jdbc.execute("""
                CREATE TABLE ticket_verification_jobs (
                    id bigserial PRIMARY KEY,
                    ticket_id uuid NOT NULL,
                    state varchar(32) NOT NULL,
                    next_attempt_at timestamptz NOT NULL,
                    lease_until timestamptz NULL,
                    created_at timestamptz NOT NULL,
                    updated_at timestamptz NOT NULL
                )
                """);
    }

    @AfterAll
    static void stop() {
        postgres.stop();
    }

    @Test
    void queries_real_postgresql_for_ready_stale_and_finally_failed_jobs() {
        insert("11111111-1111-4111-8111-111111111111", "PENDING", NOW, null, NOW.minusSeconds(30));
        insert("22222222-2222-4222-8222-222222222222", "RETRY_PENDING", NOW.minusSeconds(1), null,
                NOW.minus(Duration.ofMinutes(10)));
        insert("33333333-3333-4333-8333-333333333333", "RUNNING", NOW, NOW.plusSeconds(30),
                NOW.minus(Duration.ofMinutes(10)));
        insert("44444444-4444-4444-8444-444444444444", "FAILED_FINAL", NOW, null,
                NOW.minus(Duration.ofMinutes(10)));
        insert("55555555-5555-4555-8555-555555555555", "FAILED_FINAL", NOW, null,
                NOW.minus(Duration.ofMinutes(10)));
        insert("55555555-5555-4555-8555-555555555555", "COMPLETED", NOW, null, NOW);
        var meters = new SimpleMeterRegistry();
        var indicator = new TicketVerificationHealthIndicator(
                jdbc, meters, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(5));

        var health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails())
                .containsEntry("readyJobs", 2L)
                .containsEntry("staleJobs", 1L)
                .containsEntry("failedFinalJobs", 1L);
    }

    private static void insert(String ticketId, String state, Instant nextAttemptAt, Instant leaseUntil,
            Instant updatedAt) {
        jdbc.update("""
                INSERT INTO ticket_verification_jobs(
                    ticket_id,state,next_attempt_at,lease_until,created_at,updated_at)
                VALUES (?,?,?,?,?,?)
                """, UUID.fromString(ticketId), state, Timestamp.from(nextAttemptAt),
                leaseUntil == null ? null : Timestamp.from(leaseUntil),
                Timestamp.from(updatedAt), Timestamp.from(updatedAt));
    }
}
