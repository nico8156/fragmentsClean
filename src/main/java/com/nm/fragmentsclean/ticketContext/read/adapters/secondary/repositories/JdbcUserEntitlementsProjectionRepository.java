package com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.ticketContext.read.UserEntitlementsReadRepository;
import com.nm.fragmentsclean.ticketContext.read.projections.UserEntitlementsView;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserEntitlementsProjectionRepository implements UserEntitlementsReadRepository {

    private final JdbcTemplate jdbc;

    public JdbcUserEntitlementsProjectionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UserEntitlementsView refreshFromTicketStatus(UUID userId, long version, Instant updatedAt) {
        int count = countConfirmedTickets(userId);

        jdbc.update("""
            INSERT INTO user_entitlements_projection (
              user_id, confirmed_tickets, version, updated_at
            )
            VALUES (?, ?, ?, ?)
            ON CONFLICT (user_id) DO UPDATE SET
              confirmed_tickets = EXCLUDED.confirmed_tickets,
              version = GREATEST(user_entitlements_projection.version, EXCLUDED.version),
              updated_at = EXCLUDED.updated_at
        """, userId, count, version, Timestamp.from(updatedAt));

        return new UserEntitlementsView(userId, count, countPublishedComments(userId), countConfirmedLikes(userId), version, updatedAt);
    }

    private int countConfirmedTickets(UUID userId) {
        Integer confirmedTickets = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM ticket_status_projection
            WHERE user_id = ?
              AND status = 'CONFIRMED'
        """, Integer.class, userId);

        return confirmedTickets == null ? 0 : confirmedTickets;
    }

    private int countPublishedComments(UUID userId) {
        Integer publishedComments = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM social_comments_projection
            WHERE author_id = ?
              AND deleted_at IS NULL
              AND moderation = 'PUBLISHED'
        """, Integer.class, userId);

        return publishedComments == null ? 0 : publishedComments;
    }

    private int countConfirmedLikes(UUID userId) {
        Integer confirmedLikes = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM social_likes_projection
            WHERE user_id = ?
              AND active = true
        """, Integer.class, userId);

        return confirmedLikes == null ? 0 : confirmedLikes;
    }

    @Override
    public UserEntitlementsView findByUserId(UUID userId) {
        SqlRowSet rs = jdbc.queryForRowSet("""
            SELECT user_id, confirmed_tickets, version, updated_at
            FROM user_entitlements_projection
            WHERE user_id = ?
        """, userId);

        if (!rs.next()) {
            return new UserEntitlementsView(
                    userId,
                    countConfirmedTickets(userId),
                    countPublishedComments(userId),
                    countConfirmedLikes(userId),
                    0L,
                    Instant.EPOCH);
        }

        UUID persistedUserId = UUID.fromString(rs.getString("user_id"));
        return new UserEntitlementsView(
                persistedUserId,
                countConfirmedTickets(persistedUserId),
                countPublishedComments(persistedUserId),
                countConfirmedLikes(persistedUserId),
                rs.getLong("version"),
                ((Timestamp) rs.getObject("updated_at")).toInstant());
    }
}
