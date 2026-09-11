package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserLifecycleStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUserJpaEntity {

  @Id private UUID id;

  @Column(name = "auth_user_id", nullable = false)
  private UUID authUserId;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "version", nullable = false)
  private long version;

  @Enumerated(EnumType.STRING)
  @Column(name = "lifecycle_status", nullable = false)
  private AppUserLifecycleStatus lifecycleStatus;

  @Column(name = "deletion_requested_at")
  private Instant deletionRequestedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected AppUserJpaEntity() {}

  public AppUserJpaEntity(
      UUID id,
      UUID authUserId,
      String displayName,
      String avatarUrl,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    this(
        id,
        authUserId,
        displayName,
        avatarUrl,
        createdAt,
        updatedAt,
        version,
        AppUserLifecycleStatus.ACTIVE,
        null,
        null);
  }

  public AppUserJpaEntity(
      UUID id,
      UUID authUserId,
      String displayName,
      String avatarUrl,
      Instant createdAt,
      Instant updatedAt,
      long version,
      AppUserLifecycleStatus lifecycleStatus,
      Instant deletionRequestedAt,
      Instant deletedAt) {
    this.id = id;
    this.authUserId = authUserId;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
    this.lifecycleStatus = lifecycleStatus;
    this.deletionRequestedAt = deletionRequestedAt;
    this.deletedAt = deletedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getAuthUserId() {
    return authUserId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }

  public AppUserLifecycleStatus getLifecycleStatus() {
    return lifecycleStatus;
  }

  public Instant getDeletionRequestedAt() {
    return deletionRequestedAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
