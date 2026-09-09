package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.observability.EditorialOperationsHealthIndicator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class EditorialOperationsHealthIndicatorTest {
    @Test void exposes_operational_debt_and_degrades_health() {
        var jdbc = new StubJdbcTemplate(2, 1, 3, 1, 2, 4, 5);
        var meters = new SimpleMeterRegistry();
        var health = new EditorialOperationsHealthIndicator(
                jdbc, meters, Clock.fixed(Instant.parse("2026-09-09T08:00:00Z"), ZoneOffset.UTC), Duration.ofMinutes(30));

        var result = health.health();

        assertThat(result.getStatus().getCode()).isEqualTo("DEGRADED");
        assertThat(result.getDetails())
                .containsEntry("degradedSources", 2L)
                .containsEntry("overdueSchedules", 3L)
                .containsEntry("staleDispatchedSchedules", 2L)
                .containsEntry("failedAnalysesLast24Hours", 5L);
        assertThat(meters.get("fragments.editorial.schedules.rejected").gauge().value()).isEqualTo(4d);
    }

    @Test void remains_up_when_no_recoverable_or_terminal_failure_exists() {
        var health = new EditorialOperationsHealthIndicator(
                new StubJdbcTemplate(0, 0, 0, 0, 0, 0, 0), new SimpleMeterRegistry(),
                Clock.systemUTC(), Duration.ofMinutes(30));

        assertThat(health.health().getStatus().getCode()).isEqualTo("UP");
    }

    private static final class StubJdbcTemplate extends JdbcTemplate {
        private final long[] values;
        private int query;
        private StubJdbcTemplate(long... values) { this.values = values; }
        @Override public void afterPropertiesSet() { }
        @Override @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            return (T) Long.valueOf(values[query++ % values.length]);
        }
    }
}
