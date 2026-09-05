package com.nm.fragmentsclean.platform.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class MessagingRuntimeHealthIndicatorTest {

    @Test
    void reports_degraded_health_when_durable_messages_are_failed_or_stale() {
        var meters = new SimpleMeterRegistry();
        var jdbc = new StubJdbcTemplate();
        jdbc.outboxPending = 4;
        jdbc.outboxFailed = 1;
        jdbc.outboxStale = 2;
        jdbc.inboxFailed = 3;
        jdbc.inboxStale = 1;
        jdbc.latestProjection = Instant.parse("2026-09-05T08:58:00Z");
        var indicator = new MessagingRuntimeHealthIndicator(
                jdbc,
                meters,
                Clock.fixed(Instant.parse("2026-09-05T09:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(5));

        var health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails())
                .containsEntry("outboxFailed", 1L)
                .containsEntry("outboxStale", 2L)
                .containsEntry("inboxFailed", 3L)
                .containsEntry("latestProjectionAgeSeconds", 120L);
        assertThat(meters.get("fragments.outbox.pending").gauge().value()).isEqualTo(4d);
        assertThat(meters.get("fragments.inbox.failed").gauge().value()).isEqualTo(3d);
    }

    @Test
    void stays_up_when_queues_have_no_failed_or_stale_durable_work() {
        var indicator = new MessagingRuntimeHealthIndicator(
                new StubJdbcTemplate(),
                new SimpleMeterRegistry(),
                Clock.fixed(Instant.parse("2026-09-05T09:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(5));

        assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
    }

    private static final class StubJdbcTemplate extends JdbcTemplate {
        long outboxPending;
        long outboxFailed;
        long outboxStale;
        long inboxFailed;
        long inboxStale;
        Instant latestProjection;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql, Class<T> requiredType) {
            if (sql.contains("MAX(changed_at)")) {
                return (T) (latestProjection == null ? null : Timestamp.from(latestProjection));
            }
            if (sql.contains("outbox_events") && sql.contains("PENDING")) return (T) Long.valueOf(outboxPending);
            if (sql.contains("outbox_events") && sql.contains("FAILED")) return (T) Long.valueOf(outboxFailed);
            if (sql.contains("inbox_messages") && sql.contains("FAILED")) return (T) Long.valueOf(inboxFailed);
            throw new AssertionError("Unexpected SQL: " + sql);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            if (sql.contains("outbox_events")) return (T) Long.valueOf(outboxStale);
            if (sql.contains("inbox_messages")) return (T) Long.valueOf(inboxStale);
            throw new AssertionError("Unexpected SQL: " + sql);
        }
    }
}
