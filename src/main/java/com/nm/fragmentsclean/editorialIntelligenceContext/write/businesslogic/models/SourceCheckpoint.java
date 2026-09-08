package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models;

import java.time.Instant;

/** Provider-neutral incremental position. It holds no provider SDK or raw payload. */
public record SourceCheckpoint(String etag, String lastExternalId, Instant lastPublishedAt) {
    public static SourceCheckpoint empty() { return new SourceCheckpoint(null, null, null); }
}
