package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketSubmissionFingerprintRegistry;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketSubmissionFingerprint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class JdbcTicketSubmissionFingerprintRegistry implements TicketSubmissionFingerprintRegistry {
    private final JdbcTemplate jdbc;

    public JdbcTicketSubmissionFingerprintRegistry(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public boolean claim(TicketSubmissionFingerprint fingerprint, UUID ticketId, UUID userId, Instant claimedAt) {
        int inserted = jdbc.update("""
                INSERT INTO ticket_submission_fingerprints(fingerprint,ticket_id,user_id,created_at)
                VALUES (?,?,?,?) ON CONFLICT DO NOTHING
                """, fingerprint.value(), ticketId, userId, Timestamp.from(claimedAt));
        if (inserted == 1) return true;
        Integer owned = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ticket_submission_fingerprints
                WHERE fingerprint=? AND ticket_id=? AND user_id=?
                """, Integer.class, fingerprint.value(), ticketId, userId);
        return owned != null && owned == 1;
    }
}
