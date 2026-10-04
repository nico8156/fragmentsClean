package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.time.Instant;
import java.util.UUID;
public record MediaCatalogView(String id, String origin, UUID mediaId, UUID resourceId, UUID ownerId,
    String status, String previewUrl, String contentType, Long size, Integer width, Integer height,
    Instant createdAt, Instant updatedAt) { }
