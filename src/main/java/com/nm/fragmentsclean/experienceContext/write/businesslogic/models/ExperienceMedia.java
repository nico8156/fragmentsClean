package com.nm.fragmentsclean.experienceContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ExperienceMedia extends AggregateRoot {
  private final UUID experienceId;
  private final UUID coffeeId;
  private UUID userId;
  private final String declaredContentType;
  private final long declaredSize;
  private final String pendingObjectKey;
  private final Instant createdAt;
  private ExperienceMediaStatus status;
  private String objectKey;
  private String contentType;
  private long size;
  private Integer width;
  private Integer height;
  private String sha256;
  private Instant updatedAt;
  private long version;

  private ExperienceMedia(Snapshot s) {
    super(s.mediaId());
    experienceId = s.experienceId(); coffeeId = s.coffeeId(); userId = s.userId(); declaredContentType = s.declaredContentType();
    declaredSize = s.declaredSize(); pendingObjectKey = s.pendingObjectKey(); status = s.status();
    objectKey = s.objectKey(); contentType = s.contentType(); size = s.size(); width = s.width();
    height = s.height(); sha256 = s.sha256(); createdAt = s.createdAt(); updatedAt = s.updatedAt(); version = s.version();
  }

  public static ExperienceMedia pending(UUID mediaId, UUID experienceId, UUID coffeeId, UUID userId,
      String contentType, long size, String pendingObjectKey, Instant now) {
    requireSupported(contentType, size);
    return new ExperienceMedia(new Snapshot(mediaId, experienceId, coffeeId, userId, normalizeType(contentType), size,
        pendingObjectKey, ExperienceMediaStatus.PENDING, null, null, 0, null, null, null, now, now, 0));
  }

  public static ExperienceMedia fromSnapshot(Snapshot snapshot) { return new ExperienceMedia(snapshot); }

  public boolean matchesIntent(UUID requestedExperienceId, UUID requestedUserId, String requestedType, long requestedSize) {
    return experienceId.equals(requestedExperienceId) && Objects.equals(userId, requestedUserId)
        && declaredContentType.equals(normalizeType(requestedType)) && declaredSize == requestedSize;
  }

  public void requireOwner(UUID requesterId) {
    if (!Objects.equals(userId, requesterId)) throw new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_FORBIDDEN", "Only the media owner may change it");
  }

  public boolean confirm(String finalObjectKey, String actualType, long actualSize, int actualWidth,
      int actualHeight, String actualSha256, Instant now) {
    if (status == ExperienceMediaStatus.AVAILABLE) {
      if (!Objects.equals(objectKey, finalObjectKey) || !Objects.equals(sha256, actualSha256)) {
        throw new BusinessCommandRejectedException("EXPERIENCE_MEDIA_CONFIRM_CONFLICT", "Media was already confirmed with another object");
      }
      return false;
    }
    if (status != ExperienceMediaStatus.PENDING) throw new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_NOT_PENDING", "Media cannot be confirmed in its current state");
    if (!"image/jpeg".equals(actualType) || actualSize <= 0 || actualWidth <= 0 || actualHeight <= 0
        || actualSha256 == null || actualSha256.isBlank()) throw new BusinessCommandRejectedException(
            "EXPERIENCE_MEDIA_INVALID", "Normalized media metadata is invalid");
    objectKey = finalObjectKey; contentType = actualType; size = actualSize; width = actualWidth;
    height = actualHeight; sha256 = actualSha256; status = ExperienceMediaStatus.AVAILABLE;
    updatedAt = now; version++; return true;
  }

  public boolean requestDeletion(UUID requesterId, Instant now) { requireOwner(requesterId); return requestDeletion(now); }
  public boolean requestDeletion(Instant now) {
    if (status == ExperienceMediaStatus.DELETED || status == ExperienceMediaStatus.DELETION_PENDING) return false;
    status = ExperienceMediaStatus.DELETION_PENDING; updatedAt = now; version++; return true;
  }
  public boolean markDeleted(Instant now) {
    if (status == ExperienceMediaStatus.DELETED) return false;
    if (status != ExperienceMediaStatus.DELETION_PENDING) throw new IllegalStateException("Media deletion was not requested");
    status = ExperienceMediaStatus.DELETED; userId = null; updatedAt = now; version++; return true;
  }

  public void registerChanged(UUID commandId, String reason, Instant clientAt, Instant now) {
    registerEvent(new ExperienceMediaChangedEvent(UUID.randomUUID(), commandId, id, experienceId,
        coffeeId, userId, status, objectKey, contentType, size, width, height, reason, version, now, clientAt));
  }

  public Snapshot snapshot() {
    return new Snapshot(id, experienceId, coffeeId, userId, declaredContentType, declaredSize, pendingObjectKey,
        status, objectKey, contentType, size, width, height, sha256, createdAt, updatedAt, version);
  }

  private static void requireSupported(String type, long size) {
    String normalized = normalizeType(type);
    if (!"image/jpeg".equals(normalized) && !"image/png".equals(normalized)) throw new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_TYPE_UNSUPPORTED", "Only JPEG and PNG are supported");
    if (size <= 0 || size > 8_000_000) throw new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_SIZE_INVALID", "Image must not exceed 8 MB");
  }
  private static String normalizeType(String value) {
    if (value == null) return "";
    String normalized = value.strip().toLowerCase(java.util.Locale.ROOT);
    return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
  }

  public record Snapshot(UUID mediaId, UUID experienceId, UUID coffeeId, UUID userId, String declaredContentType,
      long declaredSize, String pendingObjectKey, ExperienceMediaStatus status, String objectKey,
      String contentType, long size, Integer width, Integer height, String sha256, Instant createdAt,
      Instant updatedAt, long version) {}
}
