package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserLifecycleStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_users")
public class AuthUserJpaEntity {

  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AuthProvider provider;

  @Column(name = "provider_user_id", nullable = false)
  private String providerUserId;

  @Column(nullable = false)
  private String email;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  // ✅ nouveaux champs
  @Column(name = "display_name")
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "last_login_at", nullable = false)
  private Instant lastLoginAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "lifecycle_status", nullable = false)
  private AuthUserLifecycleStatus lifecycleStatus;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected AuthUserJpaEntity() {}

  public AuthUserJpaEntity(
      UUID id,
      AuthProvider provider,
      String providerUserId,
      String email,
      boolean emailVerified,
      String displayName,
      String avatarUrl,
      Instant lastLoginAt) {
    this(
        id,
        provider,
        providerUserId,
        email,
        emailVerified,
        displayName,
        avatarUrl,
        lastLoginAt,
        AuthUserLifecycleStatus.ACTIVE,
        null);
  }

  public AuthUserJpaEntity(
      UUID id,
      AuthProvider provider,
      String providerUserId,
      String email,
      boolean emailVerified,
      String displayName,
      String avatarUrl,
      Instant lastLoginAt,
      AuthUserLifecycleStatus lifecycleStatus,
      Instant deletedAt) {
    this.id = id;
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.email = email;
    this.emailVerified = emailVerified;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
    this.lastLoginAt = lastLoginAt;
    this.lifecycleStatus = lifecycleStatus;
    this.deletedAt = deletedAt;
  }

  public UUID getId() {
    return id;
  }

  public AuthProvider getProvider() {
    return provider;
  }

  public String getProviderUserId() {
    return providerUserId;
  }

  public String getEmail() {
    return email;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public AuthUserLifecycleStatus getLifecycleStatus() {
    return lifecycleStatus;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }
}
