package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.openai;

import java.util.List;

public record TicketVerificationAIResult(
        Decision decision,
        int score,
        String summary,

        Integer amountCents,
        String currency,

        String ticketDateIso,        // ISO-8601, ou null
        String merchantName,
        String merchantAddress,
        String paymentMethod,

        List<LineItem> lineItems,

        List<String> reasons,
        List<String> warnings
) {
    public enum Decision { ACCEPT, REJECT, REVIEW }

    public record LineItem(String label, Integer quantity, Integer amountCents) {}
}
