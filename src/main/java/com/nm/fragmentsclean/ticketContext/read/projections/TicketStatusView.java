package com.nm.fragmentsclean.ticketContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record TicketStatusView(
		UUID ticketId,
		UUID userId,
		String status,
		String outcome,
		String imageRef,
		String ocrText,
		Integer amountCents,
		String currency,
		Instant ticketDate,
		String merchantName,
		String merchantAddress,
		String paymentMethod,
		String rejectionReason,
		long version,
		Instant occurredAt) {
}
