package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EditorialSourceStudioCatalog {
    List<Source> listSources(); List<Signal> listSignals(UUID sourceId, int limit);
    record Source(UUID id,String name,String accessMode,String authorityLevel,String endpoint,long pollingFrequencySeconds,
                  boolean enabled,String status,Instant lastCheckedAt,Instant lastSuccessfulCheckAt,Instant nextCheckAt,
                  int failureCount,String checkpointExternalId,Instant checkpointPublishedAt,long version) { }
    record Signal(UUID id,UUID sourceId,String externalId,String title,String summary,String url,String author,
                  Instant publishedAt,Instant discoveredAt,String status) { }
}
