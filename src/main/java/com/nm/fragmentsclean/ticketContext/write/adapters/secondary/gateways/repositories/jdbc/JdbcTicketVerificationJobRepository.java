package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationJobRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationJob;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTicketVerificationJobRepository implements TicketVerificationJobRepository {
    private static final String COLUMNS = "job_id,command_id,ticket_id,user_id,ocr_text,image_ref,client_at,state,attempts,lease_owner,lease_until,next_attempt_at,last_failure,version,created_at,updated_at";
    private final JdbcTemplate jdbc;

    public JdbcTicketVerificationJobRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<TicketVerificationJob> byId(UUID jobId) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT " + COLUMNS + " FROM ticket_verification_jobs WHERE job_id=?", (rs, row) ->
                    TicketVerificationJob.reconstitute(new TicketVerificationJob.Snapshot(
                            rs.getObject("job_id", UUID.class), rs.getObject("command_id", UUID.class),
                            rs.getObject("ticket_id", UUID.class), rs.getObject("user_id", UUID.class),
                            rs.getString("ocr_text"), rs.getString("image_ref"), instant(rs.getTimestamp("client_at")),
                            TicketVerificationJob.State.valueOf(rs.getString("state")), rs.getInt("attempts"),
                            rs.getString("lease_owner"), instant(rs.getTimestamp("lease_until")),
                            instant(rs.getTimestamp("next_attempt_at")), rs.getString("last_failure"), rs.getLong("version"),
                            rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant())), jobId));
        } catch (EmptyResultDataAccessException missing) {
            return Optional.empty();
        }
    }

    @Override
    public void save(TicketVerificationJob job) {
        var s = job.snapshot();
        if (s.version() == 0) {
            jdbc.update("INSERT INTO ticket_verification_jobs (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (job_id) DO NOTHING",
                    s.jobId(), s.commandId(), s.ticketId(), s.userId(), s.ocrText(), s.imageRef(), timestamp(s.clientAt()),
                    s.state().name(), s.attempts(), s.leaseOwner(), timestamp(s.leaseUntil()), timestamp(s.nextAttemptAt()),
                    s.lastFailure(), s.version(), timestamp(s.createdAt()), timestamp(s.updatedAt()));
            return;
        }
        int updated = jdbc.update("UPDATE ticket_verification_jobs SET state=?,attempts=?,lease_owner=?,lease_until=?,next_attempt_at=?,last_failure=?,version=?,updated_at=? WHERE job_id=? AND version=?",
                s.state().name(), s.attempts(), s.leaseOwner(), timestamp(s.leaseUntil()), timestamp(s.nextAttemptAt()),
                s.lastFailure(), s.version(), timestamp(s.updatedAt()), s.jobId(), s.version() - 1);
        if (updated != 1) throw new IllegalStateException("Ticket verification job version conflict: " + s.jobId());
    }

    @Override
    public List<UUID> claimableIds(Instant now, int limit) {
        return jdbc.query("SELECT job_id FROM ticket_verification_jobs WHERE state='PENDING' OR (state='RETRY_PENDING' AND next_attempt_at<=?) OR (state='RUNNING' AND lease_until<=?) ORDER BY next_attempt_at,created_at LIMIT ?",
                (rs, row) -> rs.getObject("job_id", UUID.class), timestamp(now), timestamp(now), limit);
    }

    private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
