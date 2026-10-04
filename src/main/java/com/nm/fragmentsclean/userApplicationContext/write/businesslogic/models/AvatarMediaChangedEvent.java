package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;
/** Media lifecycle fact; profile association is distinct from ownership. */
public record AvatarMediaChangedEvent(UUID eventId,UUID commandId,UUID mediaId,UUID userId,
    UUID profileUserId,String status,String objectKey,String contentType,long size,Integer width,Integer height,
    Instant createdAt,long version,Instant occurredAt) implements DomainEvent {
    public static AvatarMediaChangedEvent from(AvatarMedia.Snapshot s,UUID commandId,UUID profileUserId){
        boolean deleted=s.status()==AvatarMediaStatus.DELETED;
        return new AvatarMediaChangedEvent(UUID.randomUUID(),commandId,s.mediaId(),deleted?null:s.userId(),
            deleted?null:profileUserId,s.status().name(),deleted?null:s.objectKey(),deleted?null:s.contentType(),
            deleted?0:s.size(),deleted?null:s.width(),deleted?null:s.height(),s.createdAt(),s.version(),s.updatedAt());
    }
}
