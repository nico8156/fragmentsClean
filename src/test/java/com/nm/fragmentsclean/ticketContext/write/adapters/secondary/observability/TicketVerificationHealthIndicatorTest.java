package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class TicketVerificationHealthIndicatorTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JdbcTemplate.class, StubJdbcTemplate::new)
            .withBean(SimpleMeterRegistry.class, SimpleMeterRegistry::new)
            .withUserConfiguration(TicketVerificationHealthIndicator.class);

    @Test
    void spring_selects_the_runtime_constructor() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(TicketVerificationHealthIndicator.class);
        });
    }

    @Test
    void degrades_when_jobs_are_stale_or_finally_failed_and_exposes_metrics() {
        var jdbc = new StubJdbcTemplate(4, 2, 1);
        var meters = new SimpleMeterRegistry();
        var indicator = new TicketVerificationHealthIndicator(
                jdbc, meters, Clock.fixed(Instant.parse("2026-09-12T10:00:00Z"), ZoneOffset.UTC),
                Duration.ofMinutes(5));

        var health = indicator.health();

        assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(health.getDetails())
                .containsEntry("readyJobs", 4L)
                .containsEntry("staleJobs", 2L)
                .containsEntry("failedFinalJobs", 1L)
                .containsEntry("staleAfterSeconds", 300L);
        assertThat(meters.get("fragments.ticket.verification.jobs.ready").gauge().value()).isEqualTo(4d);
        assertThat(meters.get("fragments.ticket.verification.jobs.stale").gauge().value()).isEqualTo(2d);
        assertThat(meters.get("fragments.ticket.verification.jobs.failed.final").gauge().value()).isEqualTo(1d);
    }

    @Test
    void stays_up_while_due_work_is_recent_and_recoverable() {
        var indicator = new TicketVerificationHealthIndicator(
                new StubJdbcTemplate(1, 0, 0), new SimpleMeterRegistry(), Clock.systemUTC(), Duration.ofMinutes(5));

        assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
    }

    private static final class StubJdbcTemplate extends JdbcTemplate {
        private final long ready;
        private final long stale;
        private final long failed;

        private StubJdbcTemplate() {
            this(0, 0, 0);
        }

        private StubJdbcTemplate(long ready, long stale, long failed) {
            this.ready = ready;
            this.stale = stale;
            this.failed = failed;
        }

        @Override public void afterPropertiesSet() { }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            if (sql.contains("FAILED_FINAL")) return (T) Long.valueOf(failed);
            if (sql.contains("updated_at")) return (T) Long.valueOf(stale);
            return (T) Long.valueOf(ready);
        }
    }
}
