package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.fake;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class FakeTicketVerificationProvider implements TicketVerificationProvider {

    public FakeTicketVerificationProvider(Mode mode) {
        this.mode = mode;
    }

    public enum Mode { APPROVE, REJECT, FAIL_RETRYABLE, FAIL_FINAL }

    private final Mode mode;

    // champs utilisés en APPROVE / REJECT
    public int amountCents = 1290;
    public String currency = "EUR";
    public Instant ticketDate = Instant.parse("2024-01-01T09:00:00Z");
    public String merchantName = "Fragments Cafe";
    public String merchantAddress = "1 rue du café, Rennes";
    public String paymentMethod = "CARD";
    public List<LineItem> lineItems = List.of(
            new LineItem("Espresso", 1, 300),
            new LineItem("Filter", 1, 990)
    );

    public String rejectReasonCode = "LOW_CONFIDENCE";
    public String rejectMessage = "Could not reliably parse receipt";
    public String failMessage = "Provider unavailable";

    public String providerTraceId = "fake-trace-001";

    public final AtomicInteger calls = new AtomicInteger(0);


    @Override
    public Result verify(String ocrText, String imageRef) {
        calls.incrementAndGet();

        return switch (mode) {
            case APPROVE -> new Approved(
                    amountCents,
                    currency,
                    ticketDate,
                    merchantName,
                    merchantAddress,
                    paymentMethod,
                    lineItems,
                    providerTraceId
            );
            case REJECT -> new Rejected(
                    rejectReasonCode,
                    rejectMessage,
                    providerTraceId
            );
            case FAIL_RETRYABLE -> new FailedRetryable(failMessage, providerTraceId);
            case FAIL_FINAL -> new FailedFinal(failMessage, providerTraceId);
        };
    }
}
