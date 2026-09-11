package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;
import java.time.Instant;
import java.util.UUID;

public class AppUser extends AggregateRoot {

  private final UUID authUserId;
  private String displayName;
  private String avatarUrl;
  private final Instant createdAt;
  private Instant updatedAt;
  private long version;
  private AppUserLifecycleStatus lifecycleStatus;
  private Instant deletionRequestedAt;
  private Instant deletedAt;

  public AppUser(
      UUID id,
      UUID authUserId,
      String displayName,
      String avatarUrl,
      Instant createdAt,
      Instant updatedAt,
      long version) {
    super(id);
    this.authUserId = authUserId;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.version = version;
    this.lifecycleStatus = AppUserLifecycleStatus.ACTIVE;
  }

  public AppUser(
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
    this(id, authUserId, displayName, avatarUrl, createdAt, updatedAt, version);
    this.lifecycleStatus = lifecycleStatus;
    this.deletionRequestedAt = deletionRequestedAt;
    this.deletedAt = deletedAt;
  }

  public static AppUser createNew(
      UUID authUserId, String displayName, String avatarUrl, Instant now) {
    UUID id = authUserId;

    var user =
        new AppUser(id, authUserId, normalizeDisplayName(displayName), avatarUrl, now, now, 0L);

    user.registerEvent(
        new AppUserCreatedEvent(
            UUID.randomUUID(),
            user.id(),
            user.authUserId(),
            user.displayName(),
            user.avatarUrl(),
            user.version(),
            now));

    return user;
  }

  public UUID authUserId() {
    return authUserId;
  }

  public String displayName() {
    return displayName;
  }

  public String avatarUrl() {
    return avatarUrl;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public long version() {
    return version;
  }

  public AppUserLifecycleStatus lifecycleStatus() {
    return lifecycleStatus;
  }

  public Instant deletionRequestedAt() {
    return deletionRequestedAt;
  }

  public Instant deletedAt() {
    return deletedAt;
  }

  public boolean requestAccountDeletion(UUID requestId, Instant now) {
    if (lifecycleStatus != AppUserLifecycleStatus.ACTIVE) return false;
    lifecycleStatus = AppUserLifecycleStatus.DELETION_REQUESTED;
    deletionRequestedAt = now;
    displayName = "Compte supprimé";
    avatarUrl = null;
    updatedAt = now;
    version++;
    registerEvent(
        new AppUserDeletionRequestedEvent(
            UUID.randomUUID(), requestId, id, authUserId, version, now));
    return true;
  }

  public boolean completeAccountDeletion(Instant now) {
    if (lifecycleStatus == AppUserLifecycleStatus.DELETED) return false;
    if (lifecycleStatus != AppUserLifecycleStatus.DELETION_REQUESTED) {
      throw new IllegalStateException("Account deletion was not requested");
    }
    lifecycleStatus = AppUserLifecycleStatus.DELETED;
    deletedAt = now;
    updatedAt = now;
    version++;
    return true;
  }

  /** Idempotent update + event if changed */
  public boolean updatePublicProfile(String newDisplayName, String newAvatarUrl, Instant now) {
    requireActive();
    boolean changed = false;

    String normalized = normalizeDisplayName(newDisplayName);
    if (normalized != null && !normalized.equals(this.displayName)) {
      this.displayName = normalized;
      changed = true;
    }

    if (newAvatarUrl != null && !newAvatarUrl.equals(this.avatarUrl)) {
      this.avatarUrl = newAvatarUrl;
      changed = true;
    }

    if (changed) {
      this.updatedAt = now;
      this.version++;

      registerEvent(
          new AppUserProfileUpdatedEvent(
              UUID.randomUUID(), this.id, this.displayName, this.avatarUrl, this.version, now));
    }

    return changed;
  }

  public boolean updateDisplayName(String newDisplayName, Instant now) {
    requireActive();
    String normalized = requireValidDisplayName(newDisplayName);
    if (normalized.equals(this.displayName)) {
      return false;
    }

    this.displayName = normalized;
    this.updatedAt = now;
    this.version++;
    registerEvent(
        new AppUserProfileUpdatedEvent(
            UUID.randomUUID(), this.id, this.displayName, this.avatarUrl, this.version, now));
    return true;
  }

  public boolean replaceAvatar(String newAvatarReference, Instant now) {
    requireActive();
    if (newAvatarReference == null || !newAvatarReference.startsWith("media:avatar:")) {
      throw new IllegalArgumentException("Owned avatar reference is required");
    }
    if (newAvatarReference.equals(avatarUrl)) return false;
    avatarUrl = newAvatarReference;
    updatedAt = now;
    version++;
    registerEvent(new AppUserProfileUpdatedEvent(
        UUID.randomUUID(), id, displayName, avatarUrl, version, now));
    return true;
  }

  public boolean removeAvatar(Instant now) {
    requireActive();
    if (avatarUrl == null) return false;
    avatarUrl = null;
    updatedAt = now;
    version++;
    registerEvent(new AppUserProfileUpdatedEvent(
        UUID.randomUUID(), id, displayName, null, version, now));
    return true;
  }

  private static String normalizeDisplayName(String name) {
    if (name == null) return null;
    String trimmed = name.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String requireValidDisplayName(String name) {
    if (name == null) {
      throw new InvalidDisplayNameException("Display name is required");
    }
    if (name.codePoints().anyMatch(Character::isISOControl)) {
      throw new InvalidDisplayNameException("Display name must not contain control characters");
    }
    String normalized = name.trim().replaceAll("\\s+", " ");
    if (normalized.length() < 2 || normalized.length() > 50) {
      throw new InvalidDisplayNameException(
          "Display name must contain between 2 and 50 characters");
    }
    return normalized;
  }

  private void requireActive() {
    if (lifecycleStatus != AppUserLifecycleStatus.ACTIVE) {
      throw new InactiveAppUserException();
    }
  }
}
