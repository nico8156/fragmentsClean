package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;

public class DeleteExperienceMediaCommandHandler implements CommandHandler<DeleteExperienceMediaCommand> {
  private final ExperienceMediaRepository media; private final DomainEventPublisher events; private final DateTimeProvider clock;
  public DeleteExperienceMediaCommandHandler(ExperienceMediaRepository media, DomainEventPublisher events, DateTimeProvider clock) { this.media=media;this.events=events;this.clock=clock; }
  @Override @Transactional public void execute(DeleteExperienceMediaCommand command) {
    var item=media.byId(command.mediaId()).orElseThrow(() -> new BusinessCommandRejectedException("EXPERIENCE_MEDIA_NOT_FOUND", "Media does not exist"));
    if (!item.snapshot().experienceId().equals(command.experienceId())) throw new BusinessCommandRejectedException("EXPERIENCE_MEDIA_MISMATCH", "Media does not belong to experience");
    var now=clock.now(); if(!item.requestDeletion(command.userId(),now))return; media.save(item);
    item.registerChanged(command.commandId(),"DELETION_REQUESTED",command.clientAt(),now); ExperienceEvents.publish(item,events);
  }
}
