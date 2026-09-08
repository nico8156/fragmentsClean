package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public final class JdbcEditorialSourceRepository implements EditorialSourceRepository {
    private final JdbcTemplate jdbc;
    public JdbcEditorialSourceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Optional<EditorialSource> byId(UUID sourceId) {
        try { return Optional.of(jdbc.queryForObject(select() + " WHERE source_id = ?", (rs, row) -> map(rs), sourceId)); }
        catch (EmptyResultDataAccessException ignored) { return Optional.empty(); }
    }

    @Override public List<EditorialSource> dueAt(Instant now, int limit) {
        if (limit < 1) return List.of();
        return jdbc.query(select() + " WHERE enabled = true AND next_check_at <= ? AND (lease_until IS NULL OR lease_until <= ?) ORDER BY next_check_at, source_id LIMIT ?",
                (rs, row) -> map(rs), Timestamp.from(now), Timestamp.from(now), limit);
    }

    @Override public void save(EditorialSource source) {
        var s = source.snapshot();
        if (s.version() == 0) {
            jdbc.update("INSERT INTO editorial_sources (source_id,name,access_mode,authority_level,endpoint,polling_frequency_seconds,enabled,status,next_check_at,failure_count,version) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                    s.id(), s.name(), s.accessMode().name(), s.authorityLevel().name(), s.endpoint(), s.pollingFrequency().toSeconds(), s.enabled(), s.status().name(), timestamp(s.nextCheckAt()), s.failureCount(), 0L);
            return;
        }
        int updated = jdbc.update("UPDATE editorial_sources SET enabled=?,status=?,last_checked_at=?,last_successful_check_at=?,next_check_at=?,failure_count=?,lease_owner=?,lease_until=?,checkpoint_etag=?,checkpoint_external_id=?,checkpoint_published_at=?,version=? WHERE source_id=? AND version=?",
                s.enabled(), s.status().name(), timestamp(s.lastCheckedAt()), timestamp(s.lastSuccessfulCheckAt()), timestamp(s.nextCheckAt()), s.failureCount(), s.leaseOwner(), timestamp(s.leaseUntil()), s.checkpoint().etag(), s.checkpoint().lastExternalId(), timestamp(s.checkpoint().lastPublishedAt()), s.version(), s.id(), s.version()-1);
        if (updated != 1) throw new IllegalStateException("Editorial source version conflict: " + s.id());
    }

    private static String select() { return "SELECT source_id,name,access_mode,authority_level,endpoint,polling_frequency_seconds,enabled,status,last_checked_at,last_successful_check_at,next_check_at,failure_count,lease_owner,lease_until,checkpoint_etag,checkpoint_external_id,checkpoint_published_at,version FROM editorial_sources"; }
    private static EditorialSource map(java.sql.ResultSet rs) throws java.sql.SQLException { return EditorialSource.reconstitute(new EditorialSource.Snapshot(rs.getObject("source_id", UUID.class), rs.getString("name"), EditorialSourceAccessMode.valueOf(rs.getString("access_mode")), EditorialAuthorityLevel.valueOf(rs.getString("authority_level")), rs.getString("endpoint"), Duration.ofSeconds(rs.getLong("polling_frequency_seconds")), rs.getBoolean("enabled"), EditorialSourceStatus.valueOf(rs.getString("status")), instant(rs,"last_checked_at"), instant(rs,"last_successful_check_at"), instant(rs,"next_check_at"), rs.getInt("failure_count"), rs.getString("lease_owner"), instant(rs,"lease_until"), new SourceCheckpoint(rs.getString("checkpoint_etag"),rs.getString("checkpoint_external_id"),instant(rs,"checkpoint_published_at")),rs.getLong("version"))); }
    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException { var value=rs.getTimestamp(column); return value == null ? null : value.toInstant(); }
    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
}
