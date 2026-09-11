package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;

public class ConfirmExperienceMediaCommandHandler implements CommandHandler<ConfirmExperienceMediaCommand> {
  private final ExperienceRepository experiences;
  private final ExperienceMediaRepository media;
  private final DomainEventPublisher events;
  private final DateTimeProvider clock;
  public ConfirmExperienceMediaCommandHandler(ExperienceRepository experiences, ExperienceMediaRepository media,
      DomainEventPublisher events, DateTimeProvider clock) {
    this.experiences=experiences; this.media=media; this.events=events; this.clock=clock;
  }
  @Override @Transactional
  public void execute(ConfirmExperienceMediaCommand command) {
    var experience = experiences.byId(command.experienceId()).orElseThrow(() -> new BusinessCommandRejectedException(
        "EXPERIENCE_NOT_FOUND", "Experience does not exist"));
    if (!experience.toSnapshot().userId().equals(command.userId())) throw new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_FORBIDDEN", "Only the experience owner may confirm media");
    var item = media.byId(command.mediaId()).orElseThrow(() -> new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_NOT_FOUND", "Media upload intent does not exist"));
    item.requireOwner(command.userId());
    if (!item.snapshot().experienceId().equals(command.experienceId())) throw new BusinessCommandRejectedException(
        "EXPERIENCE_MEDIA_MISMATCH", "Media does not belong to experience");
    var now = clock.now();
    if (!item.confirm(command.objectKey(), command.contentType(), command.size(), command.width(),
        command.height(), command.sha256(), now)) return;
    media.save(item); item.registerChanged(command.commandId(), "AVAILABLE", command.clientAt(), now);
    ExperienceEvents.publish(item, events);
  }
}
