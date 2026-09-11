package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/** Presigning deliberately runs after the short registration transaction. */
public final class IssueExperienceMediaUploadIntent {
  private final RegisterExperienceMediaUploadIntent register;
  private final PrivateImageStore store;
  private final DateTimeProvider clock;
  private final Duration ttl;

  public IssueExperienceMediaUploadIntent(RegisterExperienceMediaUploadIntent register,
      PrivateImageStore store, DateTimeProvider clock, Duration ttl) {
    this.register = register; this.store = store; this.clock = clock; this.ttl = ttl;
  }

  public ExperienceMediaUploadIntent issue(UUID experienceId, UUID mediaId, UUID userId,
      String contentType, long size) {
    var pending = register.register(experienceId, mediaId, userId, contentType, size).snapshot();
    if (pending.status() == ExperienceMediaStatus.AVAILABLE) {
      return new ExperienceMediaUploadIntent(mediaId, false, null, null, Map.of(), null);
    }
    if (pending.status() != ExperienceMediaStatus.PENDING) throw new com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_NOT_PENDING", "Media upload cannot be resumed");
    var target = store.presignUpload(pending.pendingObjectKey(), pending.declaredContentType(), ttl, clock.now());
    return new ExperienceMediaUploadIntent(mediaId, true, target.url().toString(), target.method(), target.headers(), target.expiresAt());
  }
}
