package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Primitive-only handoff contract; articleContext receives a copy, never a candidate aggregate. */
public record ArticleBriefV1(UUID topicCandidateId, String subject, String editorialAngle, String locale,
                             List<String> proposedTags, List<Provenance> provenance) {
    public ArticleBriefV1 {
        Objects.requireNonNull(topicCandidateId);
        subject = required(subject, "subject");
        editorialAngle = required(editorialAngle, "editorialAngle");
        locale = locale == null || locale.isBlank() ? "fr-FR" : locale.trim();
        proposedTags = List.copyOf(proposedTags == null ? List.of() : proposedTags);
        provenance = List.copyOf(provenance == null ? List.of() : provenance);
        if (provenance.isEmpty()) throw new IllegalArgumentException("Article brief requires provenance");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    public record Provenance(String sourceName, String sourceUrl, String publishedAt, String authorityLevel) {
        public Provenance {
            sourceName = required(sourceName, "sourceName");
            sourceUrl = required(sourceUrl, "sourceUrl");
            authorityLevel = required(authorityLevel, "authorityLevel");
        }
    }
}
