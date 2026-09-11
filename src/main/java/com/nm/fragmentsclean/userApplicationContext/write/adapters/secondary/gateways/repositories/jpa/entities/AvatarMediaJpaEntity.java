package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMedia;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMediaStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name="avatar_media") @Table(name="user_avatar_media")
public class AvatarMediaJpaEntity {
  @Id @Column(name="media_id") private UUID mediaId;
  @Column(name="user_id") private UUID userId;
  @Column(name="declared_content_type",nullable=false) private String declaredContentType;
  @Column(name="declared_size",nullable=false) private long declaredSize;
  @Column(name="pending_object_key",nullable=false) private String pendingObjectKey;
  @Enumerated(EnumType.STRING) @Column(nullable=false) private AvatarMediaStatus status;
  @Column(name="object_key") private String objectKey;
  @Column(name="content_type") private String contentType;
  @Column(name="size_bytes",nullable=false) private long size;
  private Integer width;
  private Integer height;
  private String sha256;
  @Column(name="created_at",nullable=false) private Instant createdAt;
  @Column(name="updated_at",nullable=false) private Instant updatedAt;
  @Column(nullable=false) private long version;
  protected AvatarMediaJpaEntity(){}
  public AvatarMediaJpaEntity(AvatarMedia.Snapshot s){mediaId=s.mediaId();userId=s.userId();declaredContentType=s.declaredContentType();declaredSize=s.declaredSize();pendingObjectKey=s.pendingObjectKey();status=s.status();objectKey=s.objectKey();contentType=s.contentType();size=s.size();width=s.width();height=s.height();sha256=s.sha256();createdAt=s.createdAt();updatedAt=s.updatedAt();version=s.version();}
  public AvatarMedia.Snapshot snapshot(){return new AvatarMedia.Snapshot(mediaId,userId,declaredContentType,declaredSize,pendingObjectKey,status,objectKey,contentType,size,width,height,sha256,createdAt,updatedAt,version);}
}
