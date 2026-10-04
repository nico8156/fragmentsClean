package com.nm.fragmentsclean.platform.eventing.contracts;
import java.time.Instant;
import java.util.UUID;
/** Stable primitive avatar lifecycle contract, independent of the profile contract. */
public record AvatarMediaChangedIntegrationEvent(UUID eventId,UUID commandId,UUID mediaId,UUID userId,
    UUID profileUserId,String status,String objectKey,String contentType,long size,Integer width,Integer height,
    Instant createdAt,long version,Instant occurredAt) { }
