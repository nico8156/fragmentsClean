package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "tickets") // table "tickets"
@Table(name = "tickets")
@Getter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class TicketJpaEntity {

    @Id
    private UUID ticketId;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private Ticket.TicketStatus status;

    @Column(columnDefinition = "text")
    private String ocrText;

    private String imageRef;

    private Integer amountCents;

    private String currency;

    private Instant ticketDate;

    private String merchantName;

    @Column(columnDefinition = "text")
    private String merchantAddress;

    private String paymentMethod;

    /**
     * JSON sérialisé (nullable). On le garde simple pour le write-side.
     * Exemple: [{"label":"...","quantity":1,"amountCents":350}]
     */
    @Column(columnDefinition = "text")
    private String lineItemsJson;

    @Column(columnDefinition = "text")
    private String rejectionReason;

    private Instant createdAt;
    private Instant updatedAt;

    private long version;

    public TicketJpaEntity(
            UUID ticketId,
            UUID userId,
            Ticket.TicketStatus status,
            String ocrText,
            String imageRef,
            Integer amountCents,
            String currency,
            Instant ticketDate,
            String merchantName,
            String merchantAddress,
            String paymentMethod,
            String lineItemsJson,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.status = status;
        this.ocrText = ocrText;
        this.imageRef = imageRef;
        this.amountCents = amountCents;
        this.currency = currency;
        this.ticketDate = ticketDate;
        this.merchantName = merchantName;
        this.merchantAddress = merchantAddress;
        this.paymentMethod = paymentMethod;
        this.lineItemsJson = lineItemsJson;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }
}
