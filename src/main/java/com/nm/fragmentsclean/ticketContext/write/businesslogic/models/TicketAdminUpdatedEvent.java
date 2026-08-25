package com.nm.fragmentsclean.ticketContext.write.businesslogic.models;

import java.time.Instant;
import java.util.UUID;

public record TicketAdminUpdatedEvent(UUID eventId, UUID commandId, UUID ticketId, UUID userId,
		String status, String ocrText, String imageRef, Integer amountCents, String currency,
		Instant ticketDate, String merchantName, String merchantAddress, String paymentMethod,
		String rejectionReason, long version, UUID actorUserId, Instant occurredAt) implements com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent { }
