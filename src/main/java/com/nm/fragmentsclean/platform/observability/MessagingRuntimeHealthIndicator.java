package com.nm.fragmentsclean.platform.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("messagingRuntimeHealth")
public class MessagingRuntimeHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Duration staleAfter;

    public MessagingRuntimeHealthIndicator(
            JdbcTemplate jdbc,
            MeterRegistry meters,
            @Value("${fragments.messaging.health.stale-after-seconds:300}") long staleAfterSeconds) {
        this(jdbc, meters, Clock.systemUTC(), Duration.ofSeconds(Math.max(30, staleAfterSeconds)));
    }

    MessagingRuntimeHealthIndicator(JdbcTemplate jdbc, MeterRegistry meters, Clock clock, Duration staleAfter) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.staleAfter = staleAfter;
        registerGauges(meters);
    }

    @Override
    public Health health() {
        Snapshot snapshot = snapshot();
        boolean degraded = snapshot.outboxFailed > 0
                || snapshot.outboxStale > 0
                || snapshot.inboxFailed > 0
                || snapshot.inboxStale > 0;
        Health.Builder result = degraded ? Health.status("DEGRADED") : Health.up();
        return result
                .withDetail("outboxPending", snapshot.outboxPending)
                .withDetail("outboxFailed", snapshot.outboxFailed)
                .withDetail("outboxStale", snapshot.outboxStale)
                .withDetail("inboxFailed", snapshot.inboxFailed)
                .withDetail("inboxStale", snapshot.inboxStale)
                .withDetail("latestProjectionAgeSeconds", snapshot.latestProjectionAgeSeconds)
                .withDetail("staleAfterSeconds", staleAfter.toSeconds())
                .build();
    }

    private void registerGauges(MeterRegistry meters) {
        Gauge.builder("fragments.outbox.pending", this, ignored -> count("SELECT COUNT(*) FROM outbox_events WHERE status = 'PENDING'"))
                .description("Pending transactional outbox events").register(meters);
        Gauge.builder("fragments.outbox.failed", this, ignored -> count("SELECT COUNT(*) FROM outbox_events WHERE status = 'FAILED'"))
                .description("Failed transactional outbox events").register(meters);
        Gauge.builder("fragments.inbox.failed", this, ignored -> count("SELECT COUNT(*) FROM inbox_messages WHERE status = 'FAILED'"))
                .description("Failed inbox messages").register(meters);
        Gauge.builder("fragments.projection.latest.age", this, ignored -> latestProjectionAgeSeconds())
                .baseUnit("seconds").description("Age of the latest projection sync event").register(meters);
    }

    private Snapshot snapshot() {
        Timestamp staleBefore = Timestamp.from(clock.instant().minus(staleAfter));
        return new Snapshot(
                count("SELECT COUNT(*) FROM outbox_events WHERE status = 'PENDING'"),
                count("SELECT COUNT(*) FROM outbox_events WHERE status = 'FAILED'"),
                countBefore("SELECT COUNT(*) FROM outbox_events WHERE status = 'PENDING' AND created_at < ?", staleBefore),
                count("SELECT COUNT(*) FROM inbox_messages WHERE status = 'FAILED'"),
                countBefore("SELECT COUNT(*) FROM inbox_messages WHERE status = 'RECEIVED' AND received_at < ?", staleBefore),
                latestProjectionAgeSeconds());
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private long countBefore(String sql, Timestamp threshold) {
        Long value = jdbc.queryForObject(sql, Long.class, threshold);
        return value == null ? 0 : value;
    }

    private long latestProjectionAgeSeconds() {
        Timestamp latest = jdbc.queryForObject("SELECT MAX(changed_at) FROM projection_sync_events", Timestamp.class);
        if (latest == null) return 0;
        return Math.max(0, Duration.between(latest.toInstant(), Instant.now(clock)).toSeconds());
    }

    private record Snapshot(long outboxPending, long outboxFailed, long outboxStale,
                            long inboxFailed, long inboxStale, long latestProjectionAgeSeconds) {}
}
