package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.time.Instant;
import java.util.UUID;
/** Internal projection input. Storage references never form part of the HTTP contract. */
public record MediaCatalogEntry(String origin, UUID mediaId, UUID resourceId, UUID ownerId,
    String status, String objectKey, String contentType, long size, Integer width, Integer height,
    Instant createdAt, Instant updatedAt, long version) { }
