package com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways;

import java.time.Instant;
import java.util.List;

public interface TicketVerificationProvider {

    Result verify(String ocrText, String imageRef);

    sealed interface Result permits Approved, Rejected, FailedRetryable, FailedFinal {}

    record Approved(
            int amountCents,
            String currency,
            Instant ticketDate,
            String merchantName,
            String merchantAddress,
            String paymentMethod,
            List<LineItem> lineItems,
            String providerTraceId
    ) implements Result {}

    record Rejected(
            String reasonCode,
            String message,
            String providerTraceId
    ) implements Result {}

    record FailedRetryable(String message, String providerTraceId) implements Result {}
    record FailedFinal(String message, String providerTraceId) implements Result {}

    record LineItem(String label, Integer quantity, Integer amountCents) {}
}
