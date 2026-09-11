package com.nm.fragmentsclean.ticketContext.write.businesslogic.models;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

public record TicketSubmissionFingerprint(String value) {
    public static Optional<TicketSubmissionFingerprint> fromOcr(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) return Optional.empty();
        String normalized = ocrText.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return Optional.of(new TicketSubmissionFingerprint("v1:" + HexFormat.of().formatHex(digest)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("MD5 must be available", impossible);
        }
    }
}
