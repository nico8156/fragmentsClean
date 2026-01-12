//package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.openai;
//
//import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.converter.BeanOutputConverter;
//import org.springframework.core.ParameterizedTypeReference;
//import org.springframework.util.StringUtils;
//import java.time.Instant;
//import java.time.OffsetDateTime;
//import java.time.format.DateTimeParseException;
//import java.util.List;
//import java.util.UUID;
//
//
//public class OpenAiTicketVerificationProvider implements TicketVerificationProvider {
//
//    private static final int MAX_OCR_LENGTH = 20_000;
//
//    private final ChatClient chatClient;
//
//    public OpenAiTicketVerificationProvider(ChatClient chatClient) {
//        this.chatClient = chatClient;
//    }
//
//    @Override
//    public Result verify(String ocrText, String imageRef) {
//
//
//        final String traceId = newTraceId(imageRef);
//
//        if (!StringUtils.hasText(ocrText)) {
//            return new Rejected("EMPTY_OCR", "OCR text is empty", traceId);
//        }
//        if (ocrText.length() > MAX_OCR_LENGTH) {
//            return new Rejected("OCR_TOO_LONG", "OCR text exceeds max length", traceId);
//        }
//
//        var converter = new BeanOutputConverter<>(
//                new ParameterizedTypeReference<TicketVerificationAIResult>() {}
//        );
//
//        String format = converter.getFormat(); // instructions JSON pour le modèle
//
//        try {
//            TicketVerificationAIResult ai =
//                    chatClient.prompt()
//                            .system("""
//                    You are a receipt/ticket verification engine.
//                    Never invent values. Use null if unknown.
//                """)
//                            .user(u -> u.text("""
//                    Return JSON following this exact format:
//                    {format}
//
//                    Rules:
//                    - decision: ACCEPT if score >= 85
//                    - decision: REVIEW if score 60..84
//                    - decision: REJECT otherwise
//                    - score: 0..100 confidence
//
//                    Context:
//                    - imageRef: {imageRef}
//
//                    OCR:
//                    ```{ocr}
//                """)
//                                    .param("format", format)
//                                    .param("imageRef", imageRef == null ? "" : imageRef)
//                                    .param("ocr", ocrText))
//                            .call()
//                            .entity(converter);
//
//            return mapToPortResult(ai, traceId);
//
//        } catch (Exception e) {
//            // Classification simple : tu peux raffiner selon exceptions HTTP/429/5xx si tu veux.
//            // Ici: on considère retryable par défaut (réseau / provider).
//            return new FailedRetryable("OpenAI call failed: " + safeMsg(e), traceId);
//        }
//    }
//
//    private Result mapToPortResult(TicketVerificationAIResult ai, String traceId) {
//        // 1) Hard business guards (le LLM n’a pas le dernier mot)
//        if (ai == null) {
//            return new FailedRetryable("Empty AI response", traceId);
//        }
//
//        // Champs minimaux pour approuver
//        boolean missingTotal = (ai.amountCents() == null);
//        boolean missingCurrency = !StringUtils.hasText(ai.currency());
//        boolean missingMerchant = !StringUtils.hasText(ai.merchantName());
//
//        // 2) Parsing date
//        Instant ticketDate = parseInstantOrNull(ai.ticketDateIso());
//        if (ticketDate == null && ai.decision() == TicketVerificationAIResult.Decision.ACCEPT) {
//            // Si le modèle “ACCEPT” mais pas de date, on downgrade en REVIEW via reject/review selon ton produit.
//            // Ton port n’a pas REVIEW: donc on choisit Rejected ou Approved? Je te propose Rejected pour sûreté.
//            return new Rejected("DATE_MISSING", "Ticket date missing/invalid", traceId);
//        }
//
//        // 3) Si l’IA rejette
//        if (ai.decision() == TicketVerificationAIResult.Decision.REJECT) {
//            String code = firstCode(ai.reasons(), "REJECTED_BY_PROVIDER");
//            String msg = StringUtils.hasText(ai.summary()) ? ai.summary() : "Rejected";
//            return new Rejected(code, msg, traceId);
//        }
//
//        // 4) Si l’IA est en REVIEW => on mappe vers Rejected “NEEDS_REVIEW”
//        // (car ton port n’a pas de REVIEW). Alternative : Rejected code explicite.
//        if (ai.decision() == TicketVerificationAIResult.Decision.REVIEW) {
//            String code = firstCode(ai.reasons(), "NEEDS_REVIEW");
//            String msg = StringUtils.hasText(ai.summary()) ? ai.summary() : "Needs review";
//            return new Rejected(code, msg, traceId);
//        }
//
//        // 5) Si l’IA veut ACCEPT mais champs critiques manquants => on rejette ou on failed final selon ton choix.
//        if (missingTotal) {
//            return new Rejected("TOTAL_MISSING", "Total amount missing", traceId);
//        }
//        if (missingCurrency) {
//            return new Rejected("CURRENCY_MISSING", "Currency missing", traceId);
//        }
//        if (missingMerchant) {
//            return new Rejected("MERCHANT_MISSING", "Merchant name missing", traceId);
//        }
//
//        // 6) Normalisation line items
//        List<LineItem> lineItems = (ai.lineItems() == null)
//                ? List.of()
//                : ai.lineItems().stream()
//                .map(li -> new LineItem(
//                        safeStr(li.label()),
//                        li.quantity(),
//                        li.amountCents()
//                ))
//                .toList();
//
//        // 7) Approved
//        return new Approved(
//                ai.amountCents(),
//                ai.currency(),
//                ticketDate,
//                safeStr(ai.merchantName()),
//                safeStr(ai.merchantAddress()),
//                safeStr(ai.paymentMethod()),
//                lineItems,
//                traceId
//        );
//    }
//
//    private static String newTraceId(String imageRef) {
//        // stable enough; tu peux aussi mettre un hash(imageRef) si tu veux.
//        return "openai-" + UUID.randomUUID();
//    }
//
//    private static String firstCode(List<String> codes, String fallback) {
//        if (codes == null || codes.isEmpty()) return fallback;
//        String c = codes.getFirst();
//        return StringUtils.hasText(c) ? c : fallback;
//    }
//
//    private static Instant parseInstantOrNull(String iso) {
//        if (!StringUtils.hasText(iso)) return null;
//        try {
//            // accepte 2025-12-19T14:32:00+01:00
//            return OffsetDateTime.parse(iso).toInstant();
//        } catch (DateTimeParseException ignored) {
//            try {
//                // accepte 2025-12-19T14:32:00Z
//                return Instant.parse(iso);
//            } catch (DateTimeParseException ignored2) {
//                return null;
//            }
//        }
//    }
//
//    private static String safeStr(String s) {
//        return s == null ? "" : s;
//    }
//
//    private static String safeMsg(Exception e) {
//        String m = e.getMessage();
//        return m == null ? e.getClass().getSimpleName() : m;
//    }
//}
