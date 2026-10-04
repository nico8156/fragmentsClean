package com.nm.fragmentsclean.mediaCatalogContext.read;
import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
public final class MediaCatalogProjectionHandler {
    private final MediaCatalogProjection projection;
    public MediaCatalogProjectionHandler(MediaCatalogProjection projection) { this.projection=projection; }
    public void handle(ExperienceIntegrationEvents.MediaChanged e) {
        boolean deleted="DELETED".equals(e.status());
        projection.apply(new MediaCatalogEntry("EXPERIENCE", e.mediaId(), deleted?null:e.experienceId(),
            deleted?null:e.userId(), e.status(), deleted?null:e.objectKey(), deleted?null:e.contentType(),
            deleted?0:e.size(), deleted?null:e.width(), deleted?null:e.height(), e.createdAt(), e.occurredAt(), e.version()));
    }
}
