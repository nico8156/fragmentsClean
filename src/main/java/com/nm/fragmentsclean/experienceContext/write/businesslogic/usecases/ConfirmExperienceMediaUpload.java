package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.DurableCommandExecutor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaObjectKeys;
import java.time.Instant;
import java.util.UUID;

/** Downloads and normalizes outside a transaction, then submits one durable completion command. */
public final class ConfirmExperienceMediaUpload {
  private static final PrivateImageStore.ImageRules RULES = new PrivateImageStore.ImageRules(
      8_000_000, 40_000_000, 1600, 1600, false, .84f);
  private final ExperienceMediaRepository media;
  private final PrivateImageStore store;
  private final PrivateMediaObjectKeys keys;
  private final DurableCommandExecutor durable;
  private final ConfirmExperienceMediaCommandHandler handler;
  public ConfirmExperienceMediaUpload(ExperienceMediaRepository media, PrivateImageStore store,
      PrivateMediaObjectKeys keys, DurableCommandExecutor durable, ConfirmExperienceMediaCommandHandler handler) {
    this.media=media; this.store=store; this.keys=keys; this.durable=durable; this.handler=handler;
  }
  public void confirm(UUID commandId, UUID experienceId, UUID mediaId, UUID userId, Instant clientAt) {
    var pending = media.inspect(mediaId).orElseThrow(() -> new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_NOT_FOUND", "Media upload intent does not exist"));
    if (!java.util.Objects.equals(pending.userId(), userId) || !pending.experienceId().equals(experienceId)) {
      throw new BusinessCommandRejectedException("EXPERIENCE_MEDIA_FORBIDDEN", "Media upload is not owned by requester");
    }
    PrivateImageStore.ProcessedImage processed;
    if (pending.status() == ExperienceMediaStatus.AVAILABLE) {
      processed = new PrivateImageStore.ProcessedImage(pending.objectKey(), pending.contentType(),
          pending.size(), pending.width(), pending.height(), pending.sha256());
    } else if (pending.status() == ExperienceMediaStatus.PENDING) {
      processed = store.normalize(pending.pendingObjectKey(), keys.experience(experienceId, mediaId),
          pending.declaredContentType(), RULES);
    } else {
      throw new BusinessCommandRejectedException("EXPERIENCE_MEDIA_NOT_PENDING", "Media upload cannot be confirmed");
    }
    var command = new ConfirmExperienceMediaCommand(commandId, experienceId, mediaId, userId,
        processed.objectKey(), processed.contentType(), processed.size(), processed.width(),
        processed.height(), processed.sha256(), clientAt);
    durable.execute(command, () -> handler.execute(command));
    store.delete(pending.pendingObjectKey());
  }
}
