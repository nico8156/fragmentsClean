package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSourceStudioCatalog;
import com.nm.fragmentsclean.editorialIntelligenceContext.read.EditorialSourceCatalog;
import java.util.UUID;

public final class EditorialIntelligenceStudioCatalogAdapter implements EditorialSourceStudioCatalog {
 private final EditorialSourceCatalog catalog; public EditorialIntelligenceStudioCatalogAdapter(EditorialSourceCatalog catalog){this.catalog=catalog;}
 public java.util.List<Source> listSources(){return catalog.listSources().stream().map(s->new Source(s.id(),s.name(),s.accessMode(),s.authorityLevel(),s.endpoint(),s.pollingFrequencySeconds(),s.enabled(),s.status(),s.lastCheckedAt(),s.lastSuccessfulCheckAt(),s.nextCheckAt(),s.failureCount(),s.checkpointExternalId(),s.checkpointPublishedAt(),s.version())).toList();}
 public java.util.List<Signal> listSignals(UUID id,int limit){return catalog.listSignals(id,limit).stream().map(s->new Signal(s.id(),s.sourceId(),s.externalId(),s.title(),s.summary(),s.url(),s.author(),s.publishedAt(),s.discoveredAt(),s.status())).toList();}
}
