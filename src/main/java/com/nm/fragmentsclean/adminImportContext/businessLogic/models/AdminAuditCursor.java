package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Keyset cursor preserving PostgreSQL timestamp precision. */
public record AdminAuditCursor(Instant occurredAt, UUID id) {
    public static AdminAuditCursor parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            if (value.length() > 512) throw new IllegalArgumentException();
            String decoded = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            int split = decoded.lastIndexOf('|');
            return new AdminAuditCursor(Instant.parse(decoded.substring(0, split)), UUID.fromString(decoded.substring(split + 1)));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("Invalid admin audit cursor");
        }
    }
    public String encode() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((occurredAt + "|" + id).getBytes(StandardCharsets.UTF_8));
    }
}
