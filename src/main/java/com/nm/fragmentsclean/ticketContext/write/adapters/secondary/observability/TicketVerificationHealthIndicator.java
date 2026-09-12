package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("ticketVerificationHealth")
public final class TicketVerificationHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Duration staleAfter;

    @Autowired
    public TicketVerificationHealthIndicator(
            JdbcTemplate jdbc,
            MeterRegistry meters,
            @Value("${ticketverify.health.stale-after-seconds:300}") long staleAfterSeconds) {
        this(jdbc, meters, Clock.systemUTC(), Duration.ofSeconds(Math.max(30, staleAfterSeconds)));
    }

    TicketVerificationHealthIndicator(JdbcTemplate jdbc, MeterRegistry meters, Clock clock, Duration staleAfter) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.staleAfter = staleAfter;
        Gauge.builder("fragments.ticket.verification.jobs.ready", this, ignored -> readyJobs())
                .description("Ticket verification jobs ready to be claimed").register(meters);
        Gauge.builder("fragments.ticket.verification.jobs.stale", this, ignored -> staleJobs())
                .description("Ticket verification jobs left claimable beyond the operational threshold").register(meters);
        Gauge.builder("fragments.ticket.verification.jobs.failed.final", this, ignored -> failedFinalJobs())
                .description("Ticket verification jobs that exhausted technical retries").register(meters);
    }

    @Override
    public Health health() {
        long ready = readyJobs();
        long stale = staleJobs();
        long failed = failedFinalJobs();
        Health.Builder result = stale > 0 || failed > 0 ? Health.status("DEGRADED") : Health.up();
        return result
                .withDetail("readyJobs", ready)
                .withDetail("staleJobs", stale)
                .withDetail("failedFinalJobs", failed)
                .withDetail("staleAfterSeconds", staleAfter.toSeconds())
                .build();
    }

    private long readyJobs() {
        Timestamp now = Timestamp.from(Instant.now(clock));
        return count("""
                SELECT COUNT(*) FROM ticket_verification_jobs
                WHERE state = 'PENDING'
                   OR (state = 'RETRY_PENDING' AND next_attempt_at <= ?)
                   OR (state = 'RUNNING' AND lease_until <= ?)
                """, now, now);
    }

    private long staleJobs() {
        Instant now = Instant.now(clock);
        Timestamp current = Timestamp.from(now);
        Timestamp staleBefore = Timestamp.from(now.minus(staleAfter));
        return count("""
                SELECT COUNT(*) FROM ticket_verification_jobs
                WHERE updated_at <= ?
                  AND (state = 'PENDING'
                    OR (state = 'RETRY_PENDING' AND next_attempt_at <= ?)
                    OR (state = 'RUNNING' AND lease_until <= ?))
                """, staleBefore, current, current);
    }

    private long failedFinalJobs() {
        return count("""
                SELECT COUNT(*) FROM ticket_verification_jobs failed
                WHERE failed.state = 'FAILED_FINAL'
                  AND NOT EXISTS (
                    SELECT 1 FROM ticket_verification_jobs recovered
                    WHERE recovered.ticket_id = failed.ticket_id
                      AND recovered.state = 'COMPLETED'
                      AND recovered.created_at > failed.updated_at)
                """);
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }
}
