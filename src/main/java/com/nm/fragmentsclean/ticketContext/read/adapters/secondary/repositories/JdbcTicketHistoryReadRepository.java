package com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.ticketContext.read.TicketHistoryReadRepository;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistoryEntry;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistoryItemView;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistorySlice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcTicketHistoryReadRepository implements TicketHistoryReadRepository {
    private final JdbcTemplate jdbc;

    public JdbcTicketHistoryReadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TicketHistorySlice pageByUserId(UUID userId, Long beforePosition, int limit) {
        String cursorClause = beforePosition == null ? "" : " AND history_position < ?";
        Object[] args = beforePosition == null
                ? new Object[]{userId, limit + 1}
                : new Object[]{userId, beforePosition, limit + 1};
        List<TicketHistoryEntry> rows = jdbc.query("""
                SELECT history_position, ticket_id, status, outcome, amount_cents, currency,
                       ticket_date, merchant_name, merchant_address, rejection_reason,
                       version, occurred_at
                FROM ticket_status_projection
                WHERE user_id = ?
                  AND status <> 'DELETED'
                """ + cursorClause + " ORDER BY history_position DESC LIMIT ?",
                (rs, rowNum) -> new TicketHistoryEntry(
                        rs.getLong("history_position"),
                        new TicketHistoryItemView(
                                rs.getObject("ticket_id", UUID.class),
                                rs.getString("status"),
                                rs.getString("outcome"),
                                (Integer) rs.getObject("amount_cents"),
                                rs.getString("currency"),
                                instant((Timestamp) rs.getObject("ticket_date")),
                                rs.getString("merchant_name"),
                                rs.getString("merchant_address"),
                                rs.getString("rejection_reason"),
                                rs.getLong("version"),
                                instant((Timestamp) rs.getObject("occurred_at")))),
                args);
        boolean hasMore = rows.size() > limit;
        List<TicketHistoryEntry> entries = hasMore ? List.copyOf(rows.subList(0, limit)) : rows;
        Long next = hasMore && !entries.isEmpty()
                ? entries.getLast().historyPosition()
                : null;
        return new TicketHistorySlice(entries, next);
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
