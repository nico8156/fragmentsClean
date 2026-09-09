package com.nm.fragmentsclean.editorialIntelligenceContext.read;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read-side boundary for the Studio cockpit. It never exposes the write aggregate. */
public interface EditorialSourceCatalog {
    List<EditorialSourceView> listSources();
    List<SourceSignalView> listSignals(UUID sourceId, int limit);

    record EditorialSourceView(UUID id, String name, String accessMode, String authorityLevel, String endpoint,
                               long pollingFrequencySeconds, boolean enabled, String status, Instant lastCheckedAt,
                               Instant lastSuccessfulCheckAt, Instant nextCheckAt, int failureCount,
                               String checkpointExternalId, Instant checkpointPublishedAt, long version) { }
    record SourceSignalView(UUID id, UUID sourceId, String externalId, String title, String summary, String url,
                            String author, Instant publishedAt, Instant discoveredAt, String status) { }
}
