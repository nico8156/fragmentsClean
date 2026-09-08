package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable normalized discovery fact. Provider payloads never enter this model. */
public record SourceSignal(UUID id, UUID sourceId, String externalId, String title, String summary,
                           String url, String author, Instant publishedAt, Instant discoveredAt,
                           String fingerprint) {
    public SourceSignal {
        Objects.requireNonNull(id, "id"); Objects.requireNonNull(sourceId, "sourceId");
        externalId = required(externalId, "externalId"); title = required(title, "title");
        url = required(url, "url"); Objects.requireNonNull(discoveredAt, "discoveredAt");
        fingerprint = required(fingerprint, "fingerprint");
    }
    private static String required(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank"); return value; }
}
