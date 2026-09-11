package com.nm.fragmentsclean.authenticationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;
import java.time.Instant;
import java.util.UUID;

public class AuthUser extends AggregateRoot {

  private final AuthProvider provider;
  private String providerUserId;
  private String email;
  private boolean emailVerified;

  // ✅ NOUVEAUX CHAMPS
  private String displayName;
  private String avatarUrl;

  private Instant lastLoginAt;
  private AuthUserLifecycleStatus lifecycleStatus;
  private Instant deletedAt;

  public AuthUser(
      UUID id,
      AuthProvider provider,
      String providerUserId,
      String email,
      boolean emailVerified,

      // ✅ nouveaux paramètres
      String displayName,
      String avatarUrl,
      Instant lastLoginAt) {

    super(id);
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.email = email;
    this.emailVerified = emailVerified;

    this.displayName = displayName;
    this.avatarUrl = avatarUrl;

    this.lastLoginAt = lastLoginAt;
    this.lifecycleStatus = AuthUserLifecycleStatus.ACTIVE;
  }

  public AuthUser(
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
    this(id, provider, providerUserId, email, emailVerified, displayName, avatarUrl, lastLoginAt);
    this.lifecycleStatus = lifecycleStatus;
    this.deletedAt = deletedAt;
  }

  // ----------------------------------------------------------------------
  // FACTORY ENRICHIE
  // ----------------------------------------------------------------------

  public static AuthUser createNew(
      AuthProvider provider,
      String providerUserId,
      String email,
      boolean emailVerified,

      // ✅ nouveaux arguments
      String displayName,
      String avatarUrl,
      Instant now) {

    UUID id = UUID.randomUUID();

    var authUser =
        new AuthUser(
            id, provider, providerUserId, email, emailVerified, displayName, avatarUrl, now);

    // 🔥 Event enrichi
    authUser.registerEvent(AuthUserCreatedEvent.of(authUser, now));

    return authUser;
  }

  // ----------------------------------------------------------------------
  // EXISTANT CONSERVÉ
  // ----------------------------------------------------------------------

  public void markLogin(Instant now) {
    this.lastLoginAt = now;
    registerEvent(AuthUserLoggedInEvent.of(this, now));
  }

  // ----------------------------------------------------------------------
  // GETTERS
  // ----------------------------------------------------------------------

  public AuthProvider provider() {
    return provider;
  }

  public String providerUserId() {
    return providerUserId;
  }

  public String email() {
    return email;
  }

  public boolean emailVerified() {
    return emailVerified;
  }

  public Instant lastLoginAt() {
    return lastLoginAt;
  }

  // ✅ NOUVEAUX GETTERS

  public String displayName() {
    return displayName;
  }

  public String avatarUrl() {
    return avatarUrl;
  }

  public AuthUserLifecycleStatus lifecycleStatus() {
    return lifecycleStatus;
  }

  public Instant deletedAt() {
    return deletedAt;
  }

  public boolean erasePersonalData(Instant now) {
    if (lifecycleStatus == AuthUserLifecycleStatus.DELETED) return false;
    providerUserId = "deleted:" + id;
    email = "deleted+" + id + "@invalid.local";
    emailVerified = false;
    displayName = null;
    avatarUrl = null;
    lifecycleStatus = AuthUserLifecycleStatus.DELETED;
    deletedAt = now;
    return true;
  }

  // ----------------------------------------------------------------------
  // FUTUR : possibilité d’update profil
  // ----------------------------------------------------------------------

  public void updateProfile(String displayName, String avatarUrl) {
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
  }
}
