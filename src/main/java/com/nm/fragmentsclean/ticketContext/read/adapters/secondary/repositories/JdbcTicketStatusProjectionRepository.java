package com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class JdbcTicketStatusProjectionRepository {

    private final JdbcTemplate jdbc;

    public JdbcTicketStatusProjectionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    /**
     * Upsert "ANALYZING" state.
     * Idempotent / last-write-wins by version (we accept overwrite; you can add guards if needed).
     */
    public void applyAnalyzing(TicketVerifyAcceptedEvent evt) {
        jdbc.update("""
            INSERT INTO ticket_status_projection (
              ticket_id, user_id, status, outcome,
              image_ref, ocr_text,
              amount_cents, currency, ticket_date,
              merchant_name, merchant_address, payment_method,
              rejection_reason, version, occurred_at
            )
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (ticket_id) DO UPDATE SET
              user_id = EXCLUDED.user_id,
              status = EXCLUDED.status,
              outcome = EXCLUDED.outcome,
              image_ref = COALESCE(EXCLUDED.image_ref, ticket_status_projection.image_ref),
              ocr_text  = COALESCE(EXCLUDED.ocr_text,  ticket_status_projection.ocr_text),
              version = EXCLUDED.version,
              occurred_at = EXCLUDED.occurred_at
        """,
                evt.ticketId(),
                evt.userId(),
                Ticket.TicketStatus.ANALYZING.name(),
                null,
                evt.imageRef(),
                evt.ocrText(),
                null, null, null,
                null, null, null,
                null,
                evt.version(),
                ts(evt.occurredAt())
        );
    }

    /**
     * Upsert completed state (APPROVED/REJECTED/FAILED_*).
     * For FAILED_* we keep status ANALYZING (or you can set status REJECTED if you want).
     */
    public void applyCompleted(TicketVerificationCompletedEvent evt) {
        String status = switch (evt.outcome()) {
            case APPROVED -> Ticket.TicketStatus.CONFIRMED.name();
            case REJECTED -> Ticket.TicketStatus.REJECTED.name();
            case FAILED_RETRYABLE, FAILED_FINAL -> Ticket.TicketStatus.ANALYZING.name();
        };

        Integer amountCents = evt.approved() != null ? evt.approved().amountCents() : null;
        String currency = evt.approved() != null ? evt.approved().currency() : null;
        Instant ticketDate = evt.approved() != null ? evt.approved().ticketDate() : null;

        String merchantName = evt.approved() != null ? evt.approved().merchantName() : null;
        String merchantAddress = evt.approved() != null ? evt.approved().merchantAddress() : null;
        String paymentMethod = evt.approved() != null ? evt.approved().paymentMethod() : null;

        String rejectionReason = evt.rejected() != null ? evt.rejected().reasonCode() : null;

        jdbc.update("""
            INSERT INTO ticket_status_projection (
              ticket_id, user_id, status, outcome,
              image_ref, ocr_text,
              amount_cents, currency, ticket_date,
              merchant_name, merchant_address, payment_method,
              rejection_reason, version, occurred_at
            )
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT (ticket_id) DO UPDATE SET
              user_id = EXCLUDED.user_id,
              status = EXCLUDED.status,
              outcome = EXCLUDED.outcome,
              amount_cents = EXCLUDED.amount_cents,
              currency = EXCLUDED.currency,
              ticket_date = EXCLUDED.ticket_date,
              merchant_name = EXCLUDED.merchant_name,
              merchant_address = EXCLUDED.merchant_address,
              payment_method = EXCLUDED.payment_method,
              rejection_reason = EXCLUDED.rejection_reason,
              version = EXCLUDED.version,
              occurred_at = EXCLUDED.occurred_at
        """,
                evt.ticketId(),
                evt.userId(),
                status,
                evt.outcome().name(),
                null,
                null,
                amountCents,
                currency,
                ts(ticketDate),
                merchantName,
                merchantAddress,
                paymentMethod,
                rejectionReason,
                evt.version(),
                ts(evt.occurredAt())
        );
    }
}
