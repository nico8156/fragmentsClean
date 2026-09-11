package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AvatarMedia extends AggregateRoot {
  private UUID userId;
  private final String declaredContentType;
  private final long declaredSize;
  private final String pendingObjectKey;
  private final Instant createdAt;
  private AvatarMediaStatus status;
  private String objectKey;
  private String contentType;
  private long size;
  private Integer width;
  private Integer height;
  private String sha256;
  private Instant updatedAt;
  private long version;

  private AvatarMedia(Snapshot s) {
    super(s.mediaId()); userId=s.userId(); declaredContentType=s.declaredContentType(); declaredSize=s.declaredSize();
    pendingObjectKey=s.pendingObjectKey(); createdAt=s.createdAt(); status=s.status(); objectKey=s.objectKey();
    contentType=s.contentType(); size=s.size(); width=s.width(); height=s.height(); sha256=s.sha256(); updatedAt=s.updatedAt(); version=s.version();
  }
  public static AvatarMedia pending(UUID mediaId,UUID userId,String contentType,long size,String pendingKey,Instant now){String type=normalizeType(contentType);if(!"image/jpeg".equals(type)&&!"image/png".equals(type))throw new BusinessCommandRejectedException("AVATAR_TYPE_UNSUPPORTED","Only JPEG and PNG are supported");if(size<=0||size>8_000_000)throw new BusinessCommandRejectedException("AVATAR_SIZE_INVALID","Avatar must not exceed 8 MB");return new AvatarMedia(new Snapshot(mediaId,userId,type,size,pendingKey,AvatarMediaStatus.PENDING,null,null,0,null,null,null,now,now,0));}
  public static AvatarMedia fromSnapshot(Snapshot s){return new AvatarMedia(s);}
  public boolean matchesIntent(UUID owner,String type,long requestedSize){return Objects.equals(userId,owner)&&declaredContentType.equals(normalizeType(type))&&declaredSize==requestedSize;}
  public void requireOwner(UUID owner){if(!Objects.equals(userId,owner))throw new BusinessCommandRejectedException("AVATAR_FORBIDDEN","Avatar is not owned by requester");}
  public boolean confirm(String key,String type,long bytes,int w,int h,String hash,Instant now){if(status==AvatarMediaStatus.AVAILABLE){if(!Objects.equals(objectKey,key)||!Objects.equals(sha256,hash))throw new BusinessCommandRejectedException("AVATAR_CONFIRM_CONFLICT","Avatar was already confirmed with another object");return false;}if(status!=AvatarMediaStatus.PENDING)throw new BusinessCommandRejectedException("AVATAR_NOT_PENDING","Avatar cannot be confirmed");if(!"image/jpeg".equals(type)||bytes<=0||w<=0||h<=0||w!=h||hash==null||hash.isBlank())throw new BusinessCommandRejectedException("AVATAR_INVALID","Normalized avatar metadata is invalid");objectKey=key;contentType=type;size=bytes;width=w;height=h;sha256=hash;status=AvatarMediaStatus.AVAILABLE;updatedAt=now;version++;return true;}
  public boolean requestDeletion(Instant now){if(status==AvatarMediaStatus.DELETED||status==AvatarMediaStatus.DELETION_PENDING)return false;status=AvatarMediaStatus.DELETION_PENDING;updatedAt=now;version++;return true;}
  public boolean markDeleted(Instant now){if(status==AvatarMediaStatus.DELETED)return false;if(status!=AvatarMediaStatus.DELETION_PENDING)throw new IllegalStateException("Avatar deletion was not requested");status=AvatarMediaStatus.DELETED;userId=null;updatedAt=now;version++;return true;}
  public Snapshot snapshot(){return new Snapshot(id,userId,declaredContentType,declaredSize,pendingObjectKey,status,objectKey,contentType,size,width,height,sha256,createdAt,updatedAt,version);}
  private static String normalizeType(String value){if(value==null)return "";String type=value.strip().toLowerCase(java.util.Locale.ROOT);return "image/jpg".equals(type)?"image/jpeg":type;}
  public record Snapshot(UUID mediaId,UUID userId,String declaredContentType,long declaredSize,String pendingObjectKey,AvatarMediaStatus status,String objectKey,String contentType,long size,Integer width,Integer height,String sha256,Instant createdAt,Instant updatedAt,long version){}
}
