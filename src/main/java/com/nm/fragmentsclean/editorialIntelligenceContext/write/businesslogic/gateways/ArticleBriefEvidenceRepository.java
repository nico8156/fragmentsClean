package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Owning-context lookup used to snapshot evidence into an article hand-off. */
public interface ArticleBriefEvidenceRepository {
    List<Evidence> bySignalIds(List<UUID> signalIds);
    record Evidence(String sourceName, String sourceUrl, Instant publishedAt, String authorityLevel) { }
}
