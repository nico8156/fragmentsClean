package com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import com.nm.fragmentsclean.ticketContext.read.TicketStatusReadRepository;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketStatusView;

@Repository
public class JdbcTicketStatusReadRepository implements TicketStatusReadRepository {

	private final JdbcTemplate jdbc;

	public JdbcTicketStatusReadRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public TicketStatusView findById(UUID ticketId) {
		SqlRowSet rs = jdbc.queryForRowSet("""
				    SELECT ticket_id, user_id, status, outcome,
				           image_ref, ocr_text,
				           amount_cents, currency, ticket_date,
				           merchant_name, merchant_address, payment_method,
				           rejection_reason, version, occurred_at
				    FROM ticket_status_projection
				    WHERE ticket_id = ?
				""", ticketId);

		if (!rs.next())
			return null;

		return new TicketStatusView(
				UUID.fromString(rs.getString("ticket_id")),
				UUID.fromString(rs.getString("user_id")),
				rs.getString("status"),
				rs.getString("outcome"),
				rs.getString("image_ref"),
				rs.getString("ocr_text"),
				(Integer) rs.getObject("amount_cents"),
				rs.getString("currency"),
				toInstant((Timestamp) rs.getObject("ticket_date")),
				rs.getString("merchant_name"),
				rs.getString("merchant_address"),
				rs.getString("payment_method"),
				rs.getString("rejection_reason"),
				rs.getLong("version"),
				toInstant((Timestamp) rs.getObject("occurred_at")));
	}

	private Instant toInstant(Timestamp ts) {
		return ts == null ? null : ts.toInstant();
	}
}
