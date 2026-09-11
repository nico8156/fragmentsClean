package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "experience_media")
@Table(name = "experience_media")
public class ExperienceMediaJpaEntity {
  @Id @Column(name = "media_id") private UUID mediaId;
  @Column(name = "experience_id", nullable = false) private UUID experienceId;
  @Column(name = "coffee_id", nullable = false) private UUID coffeeId;
  @Column(name = "user_id") private UUID userId;
  @Column(name = "declared_content_type", nullable = false) private String declaredContentType;
  @Column(name = "declared_size", nullable = false) private long declaredSize;
  @Column(name = "pending_object_key", nullable = false) private String pendingObjectKey;
  @Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private ExperienceMediaStatus status;
  @Column(name = "object_key") private String objectKey;
  @Column(name = "content_type") private String contentType;
  @Column(name = "size_bytes", nullable = false) private long size;
  private Integer width;
  private Integer height;
  private String sha256;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;
  @Column(name = "version", nullable = false) private long version;

  protected ExperienceMediaJpaEntity() {}
  public ExperienceMediaJpaEntity(ExperienceMedia.Snapshot s) {
    mediaId=s.mediaId(); experienceId=s.experienceId(); coffeeId=s.coffeeId(); userId=s.userId(); declaredContentType=s.declaredContentType();
    declaredSize=s.declaredSize(); pendingObjectKey=s.pendingObjectKey(); status=s.status(); objectKey=s.objectKey();
    contentType=s.contentType(); size=s.size(); width=s.width(); height=s.height(); sha256=s.sha256();
    createdAt=s.createdAt(); updatedAt=s.updatedAt(); version=s.version();
  }
  public ExperienceMedia.Snapshot snapshot() {
    return new ExperienceMedia.Snapshot(mediaId, experienceId, coffeeId, userId, declaredContentType, declaredSize,
        pendingObjectKey, status, objectKey, contentType, size, width, height, sha256, createdAt, updatedAt, version);
  }
}
