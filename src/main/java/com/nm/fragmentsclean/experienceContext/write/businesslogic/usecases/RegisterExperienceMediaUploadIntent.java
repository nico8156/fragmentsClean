package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaObjectKeys;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import jakarta.transaction.Transactional;
import java.util.UUID;

public class RegisterExperienceMediaUploadIntent {
  private final ExperienceRepository experiences;
  private final ExperienceMediaRepository media;
  private final PrivateMediaObjectKeys keys;
  private final DateTimeProvider clock;
  private final int maxMedia;

  public RegisterExperienceMediaUploadIntent(ExperienceRepository experiences,
      ExperienceMediaRepository media, PrivateMediaObjectKeys keys, DateTimeProvider clock, int maxMedia) {
    this.experiences = experiences; this.media = media; this.keys = keys; this.clock = clock; this.maxMedia = maxMedia;
  }

  @Transactional
  public ExperienceMedia register(UUID experienceId, UUID mediaId, UUID userId, String contentType, long size) {
    var existing = media.byId(mediaId);
    if (existing.isPresent()) {
      if (!existing.get().matchesIntent(experienceId, userId, contentType, size)) throw new BusinessCommandRejectedException(
          "EXPERIENCE_MEDIA_ID_CONFLICT", "Media id belongs to another upload intent");
      return existing.get();
    }
    var experience = experiences.byId(experienceId).orElseThrow(() -> new BusinessCommandRejectedException(
        "EXPERIENCE_NOT_FOUND", "Experience does not exist"));
    var snapshot = experience.toSnapshot();
    if (!snapshot.userId().equals(userId)) throw new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_FORBIDDEN", "Only the experience owner may add media");
    if (snapshot.publicationStatus() == com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperiencePublicationStatus.DELETED) {
      throw new BusinessCommandRejectedException("EXPERIENCE_DELETED", "Deleted experience cannot receive media");
    }
    if (media.activeCount(experienceId) >= maxMedia) throw new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_LIMIT", "Experience media limit reached");
    var pending = ExperienceMedia.pending(mediaId, experienceId, snapshot.coffeeId(), userId, contentType, size,
        keys.pendingExperience(experienceId, mediaId), clock.now());
    media.save(pending);
    return pending;
  }
}
