package com.nm.fragmentsclean.ticketContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Ticket extends AggregateRoot {

    private final UUID userId;

    private TicketStatus status;

    private String ocrText;              // nullable
    private String imageRef;             // nullable

    private Integer amountCents;         // nullable tant que pas confirmé
    private String currency;             // "EUR" par défaut côté front
    private Instant ticketDate;          // nullable (date du ticket)
    private String merchantName;         // nullable
    private String merchantAddress;      // nullable
    private String paymentMethod;        // nullable
    private List<TicketLineItem> lineItems; // nullable/empty

    private String rejectionReason;      // nullable

    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    private Ticket(UUID ticketId,
                   UUID userId,
                   TicketStatus status,
                   String ocrText,
                   String imageRef,
                   Integer amountCents,
                   String currency,
                   Instant ticketDate,
                   String merchantName,
                   String merchantAddress,
                   String paymentMethod,
                   List<TicketLineItem> lineItems,
                   String rejectionReason,
                   Instant createdAt,
                   Instant updatedAt,
                   long version) {
        super(ticketId);
        this.userId = Objects.requireNonNull(userId, "userId");

        this.status = Objects.requireNonNull(status, "status");
        this.ocrText = ocrText;
        this.imageRef = imageRef;

        this.amountCents = amountCents;
        this.currency = (currency == null || currency.isBlank()) ? "EUR" : currency;
        this.ticketDate = ticketDate;
        this.merchantName = merchantName;
        this.merchantAddress = merchantAddress;
        this.paymentMethod = paymentMethod;
        this.lineItems = (lineItems == null) ? null : new ArrayList<>(lineItems);

        this.rejectionReason = rejectionReason;

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt;
        this.version = version;
    }

    // ---- Factories ----

    /**
     * Nouveau ticket à l'envoi (optimistic côté front: ANALYZING).
     * On le crée directement en ANALYZING côté serveur dès réception de la commande Verify.
     */
    public static Ticket createNewAnalyzing(UUID ticketId,
                                            UUID userId,
                                            String ocrText,
                                            String imageRef,
                                            Instant now) {
        return new Ticket(
                ticketId,
                userId,
                TicketStatus.ANALYZING,
                ocrText,
                imageRef,
                null,
                "EUR",
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                now,
                0L
        );
    }

    public static Ticket fromSnapshot(TicketSnapshot snap) {
        return new Ticket(
                snap.ticketId(),
                snap.userId(),
                snap.status(),
                snap.ocrText(),
                snap.imageRef(),
                snap.amountCents(),
                snap.currency(),
                snap.ticketDate(),
                snap.merchantName(),
                snap.merchantAddress(),
                snap.paymentMethod(),
                snap.lineItems(),
                snap.rejectionReason(),
                snap.createdAt(),
                snap.updatedAt(),
                snap.version()
        );
    }

    // ---- Domain behavior ----

    /**
     * Idempotent "verify request received": place en ANALYZING si pas déjà confirmé/rejeté.
     * @return true si changement, false si idempotent/no-op
     */
    public boolean markAnalyzingIfPossible(String ocrText, String imageRef, Instant serverNow) {
        if (this.status == TicketStatus.CONFIRMED || this.status == TicketStatus.REJECTED) {
            return false; // terminal -> no-op
        }
        boolean changed = false;

        // On accepte d'enrichir ocrText/imageRef si manquants (idempotent friendly)
        if (ocrText != null && !ocrText.equals(this.ocrText)) {
            this.ocrText = ocrText;
            changed = true;
        }
        if (imageRef != null && !imageRef.equals(this.imageRef)) {
            this.imageRef = imageRef;
            changed = true;
        }

        if (this.status != TicketStatus.ANALYZING) {
            this.status = TicketStatus.ANALYZING;
            changed = true;
        }

        if (changed) {
            touch(serverNow);
        }
        return changed;
    }

    /**
     * Confirme le ticket avec les données extraites (OpenAI).
     * @return true si changement, false si déjà confirmé avec même version logique (idempotent)
     */
    public boolean confirm(ConfirmResult result, Instant serverNow) {
        Objects.requireNonNull(result, "result");
        if (this.status == TicketStatus.CONFIRMED) {
            // Option: comparer un sous-ensemble de champs si tu veux une idempotence "strong"
            return false;
        }
        if (this.status == TicketStatus.REJECTED) {
            throw new IllegalStateException("Cannot confirm a rejected ticket");
        }

        this.status = TicketStatus.CONFIRMED;

        this.amountCents = result.amountCents();
        this.currency = (result.currency() == null || result.currency().isBlank()) ? "EUR" : result.currency();
        this.ticketDate = result.ticketDate();
        this.merchantName = result.merchantName();
        this.merchantAddress = result.merchantAddress();
        this.paymentMethod = result.paymentMethod();
        this.lineItems = (result.lineItems() == null) ? null : new ArrayList<>(result.lineItems());

        this.rejectionReason = null;

        touch(serverNow);
        return true;
    }

    /**
     * Rejette le ticket (raison métier / score faible / pas un ticket, etc.)
     * @return true si changement, false si déjà rejeté
     */
    public boolean reject(String reason, Instant serverNow) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
        if (this.status == TicketStatus.REJECTED) {
            return false;
        }
        if (this.status == TicketStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot reject a confirmed ticket");
        }

        this.status = TicketStatus.REJECTED;
        this.rejectionReason = reason;

        // Option: garder ce qu'on a déjà appris du marchand même en rejet
        touch(serverNow);
        return true;
    }

    private void touch(Instant serverNow) {
        this.updatedAt = Objects.requireNonNull(serverNow, "serverNow");
        this.version++;
    }

    // ---- Events (stubs, tu les cales comme pour Comment) ----

    public void registerVerifiedAcceptedEvent(UUID commandId, Instant clientAt, Instant serverNow) {
        // event "TicketVerifyAccepted" (optionnel) si tu veux tracer le passage en ANALYZING
        // registerEvent(new TicketVerifyAcceptedEvent(...));
    }

    public void registerConfirmedEvent(UUID commandId, Instant clientAt, Instant serverNow) {
        // registerEvent(new TicketConfirmedEvent(
        //      UUID.randomUUID(), commandId, this.id, this.userId, this.version, serverNow, clientAt, ...payload...
        // ));
    }

    public void registerRejectedEvent(UUID commandId, Instant clientAt, Instant serverNow) {
        // registerEvent(new TicketRejectedEvent(
        //      UUID.randomUUID(), commandId, this.id, this.userId, this.rejectionReason, this.version, serverNow, clientAt
        // ));
    }

    // ---- Snapshot ----

    public TicketSnapshot toSnapshot() {
        return new TicketSnapshot(
                this.id,
                this.userId,
                this.status,
                this.ocrText,
                this.imageRef,
                this.amountCents,
                this.currency,
                this.ticketDate,
                this.merchantName,
                this.merchantAddress,
                this.paymentMethod,
                this.lineItems == null ? null : List.copyOf(this.lineItems),
                this.rejectionReason,
                this.createdAt,
                this.updatedAt,
                this.version
        );
    }

    public record TicketSnapshot(
            UUID ticketId,
            UUID userId,
            TicketStatus status,
            String ocrText,
            String imageRef,
            Integer amountCents,
            String currency,
            Instant ticketDate,
            String merchantName,
            String merchantAddress,
            String paymentMethod,
            List<TicketLineItem> lineItems,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {}

    // ---- Value objects ----

    public enum TicketStatus {
        CAPTURED,   // si un jour tu crées un ticket "local" avant verify
        ANALYZING,
        CONFIRMED,
        REJECTED
    }

    public record TicketLineItem(
            String label,
            Integer quantity,
            Integer amountCents
    ) {}

    /**
     * Résultat "normalisé" de la vérif OpenAI (ce que ton application veut stocker).
     * (Le score / raw output OpenAI peut rester hors agrégat, ou en audit séparé)
     */
    public record ConfirmResult(
            int amountCents,
            String currency,
            Instant ticketDate,
            String merchantName,
            String merchantAddress,
            String paymentMethod,
            List<TicketLineItem> lineItems
    ) {}
}
