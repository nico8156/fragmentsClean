package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialPublicationScheduleRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialScheduleConcurrencyException;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialPublicationSchedule;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcEditorialPublicationScheduleRepository implements EditorialPublicationScheduleRepository {
    private static final String COLUMNS = "schedule_id,article_id,revision_id,operation,due_at,status,lease_owner,lease_until,rejection_reason,created_at,version";
    private final JdbcTemplate jdbc;

    public JdbcEditorialPublicationScheduleRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void save(EditorialPublicationSchedule schedule) {
        var value = schedule.snapshot();
        if (value.version() == 0) {
            jdbc.update("INSERT INTO editorial_publication_schedule(" + COLUMNS + ") VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    value.id(), value.articleId(), value.revisionId(), value.operation().name(),
                    Timestamp.from(value.dueAt()), value.status().name(), value.leaseOwner(), timestamp(value.leaseUntil()),
                    value.rejectionReason(), Timestamp.from(value.createdAt()), value.version());
            return;
        }
        int updated = jdbc.update("UPDATE editorial_publication_schedule SET status=?,lease_owner=?,lease_until=?,rejection_reason=?,version=? WHERE schedule_id=? AND version=?",
                value.status().name(), value.leaseOwner(), timestamp(value.leaseUntil()), value.rejectionReason(),
                value.version(), value.id(), value.version() - 1);
        if (updated != 1) throw new EditorialScheduleConcurrencyException();
    }

    @Override
    public Optional<EditorialPublicationSchedule> byId(UUID scheduleId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT " + COLUMNS + " FROM editorial_publication_schedule WHERE schedule_id=?", this::map, scheduleId));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public List<EditorialPublicationSchedule> claimableAt(Instant now, int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM editorial_publication_schedule WHERE (status='SCHEDULED' AND due_at<=?) OR (status='CLAIMED' AND lease_until<=?) ORDER BY due_at,schedule_id LIMIT ?",
                this::map, Timestamp.from(now), Timestamp.from(now), Math.max(1, limit));
    }

    @Override
    public List<EditorialPublicationSchedule> dispatched(int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM editorial_publication_schedule WHERE status='DISPATCHED' ORDER BY due_at,schedule_id LIMIT ?",
                this::map, Math.max(1, limit));
    }

    private EditorialPublicationSchedule map(ResultSet result, int row) throws SQLException {
        Timestamp leaseUntil = result.getTimestamp("lease_until");
        return EditorialPublicationSchedule.reconstitute(new EditorialPublicationSchedule.Snapshot(
                result.getObject("schedule_id", UUID.class), result.getObject("article_id", UUID.class),
                result.getObject("revision_id", UUID.class), EditorialPublicationSchedule.Operation.valueOf(result.getString("operation")),
                result.getTimestamp("due_at").toInstant(), EditorialPublicationSchedule.Status.valueOf(result.getString("status")),
                result.getString("lease_owner"), leaseUntil == null ? null : leaseUntil.toInstant(),
                result.getString("rejection_reason"), result.getTimestamp("created_at").toInstant(), result.getLong("version")));
    }

    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
}
