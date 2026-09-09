package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.observability;

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

/** Operational read model only; it observes editorial-owned tables and never drives domain decisions. */
@Component("editorialOperationsHealth")
public class EditorialOperationsHealthIndicator implements HealthIndicator {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Duration staleAfter;

    @Autowired
    public EditorialOperationsHealthIndicator(
            JdbcTemplate jdbc,
            MeterRegistry meters,
            @Value("${fragments.editorial.health.stale-after-minutes:30}") long staleAfterMinutes) {
        this(jdbc, meters, Clock.systemUTC(), Duration.ofMinutes(Math.max(5, staleAfterMinutes)));
    }

    public EditorialOperationsHealthIndicator(JdbcTemplate jdbc, MeterRegistry meters, Clock clock, Duration staleAfter) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.staleAfter = staleAfter;
        registerGauges(meters);
    }

    @Override
    public Health health() {
        Snapshot value = snapshot();
        boolean degraded = value.degradedSources > 0 || value.expiredSourceLeases > 0
                || value.overdueSchedules > 0 || value.expiredScheduleLeases > 0
                || value.staleDispatchedSchedules > 0 || value.rejectedSchedules > 0;
        Health.Builder health = degraded ? Health.status("DEGRADED") : Health.up();
        return health
                .withDetail("degradedSources", value.degradedSources)
                .withDetail("expiredSourceLeases", value.expiredSourceLeases)
                .withDetail("overdueSchedules", value.overdueSchedules)
                .withDetail("expiredScheduleLeases", value.expiredScheduleLeases)
                .withDetail("staleDispatchedSchedules", value.staleDispatchedSchedules)
                .withDetail("rejectedSchedules", value.rejectedSchedules)
                .withDetail("failedAnalysesLast24Hours", value.failedAnalysesLast24Hours)
                .withDetail("staleAfterMinutes", staleAfter.toMinutes())
                .build();
    }

    private void registerGauges(MeterRegistry meters) {
        Gauge.builder("fragments.editorial.sources.degraded", this, ignored -> snapshot().degradedSources)
                .description("Enabled editorial sources currently degraded").register(meters);
        Gauge.builder("fragments.editorial.source.leases.expired", this, ignored -> snapshot().expiredSourceLeases)
                .description("Editorial source consultations left with an expired lease").register(meters);
        Gauge.builder("fragments.editorial.schedules.overdue", this, ignored -> snapshot().overdueSchedules)
                .description("Editorial operations still scheduled after their execution tolerance").register(meters);
        Gauge.builder("fragments.editorial.schedule.leases.expired", this, ignored -> snapshot().expiredScheduleLeases)
                .description("Claimed editorial operations with an expired lease").register(meters);
        Gauge.builder("fragments.editorial.schedules.dispatched.stale", this, ignored -> snapshot().staleDispatchedSchedules)
                .description("Dispatched editorial operations awaiting command reconciliation too long").register(meters);
        Gauge.builder("fragments.editorial.schedules.rejected", this, ignored -> snapshot().rejectedSchedules)
                .description("Editorial operations rejected by article domain rules").register(meters);
        Gauge.builder("fragments.editorial.analysis.failed", this, ignored -> snapshot().failedAnalysesLast24Hours)
                .description("Failed editorial analysis executions during the last 24 hours").register(meters);
    }

    private Snapshot snapshot() {
        Instant now = clock.instant();
        Timestamp staleBefore = Timestamp.from(now.minus(staleAfter));
        Timestamp current = Timestamp.from(now);
        Timestamp dayBefore = Timestamp.from(now.minus(Duration.ofHours(24)));
        return new Snapshot(
                count("SELECT COUNT(*) FROM editorial_sources WHERE enabled = true AND status = 'DEGRADED'"),
                count("SELECT COUNT(*) FROM editorial_sources WHERE lease_owner IS NOT NULL AND lease_until < ?", current),
                count("SELECT COUNT(*) FROM editorial_publication_schedule WHERE status = 'SCHEDULED' AND due_at < ?", staleBefore),
                count("SELECT COUNT(*) FROM editorial_publication_schedule WHERE status = 'CLAIMED' AND lease_until < ?", current),
                count("SELECT COUNT(*) FROM editorial_publication_schedule WHERE status = 'DISPATCHED' AND due_at < ?", staleBefore),
                count("SELECT COUNT(*) FROM editorial_publication_schedule WHERE status = 'REJECTED'"),
                count("SELECT COUNT(*) FROM editorial_generation_executions WHERE outcome = 'FAILED' AND occurred_at >= ?", dayBefore));
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbc.queryForObject(sql, Long.class, arguments);
        return value == null ? 0L : value;
    }

    private record Snapshot(long degradedSources, long expiredSourceLeases, long overdueSchedules,
                            long expiredScheduleLeases, long staleDispatchedSchedules,
                            long rejectedSchedules, long failedAnalysesLast24Hours) { }
}
