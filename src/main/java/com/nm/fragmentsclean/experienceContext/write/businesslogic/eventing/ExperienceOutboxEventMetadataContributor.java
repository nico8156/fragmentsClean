package com.nm.fragmentsclean.experienceContext.write.businesslogic.eventing;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public final class ExperienceOutboxEventMetadataContributor implements OutboxEventMetadataContributor{
    @Override public Optional<OutboxEventMetadata> resolve(DomainEvent event){
        if(event instanceof ExperienceSnapshotChangedEvent e)return Optional.of(experience(e.experienceId(),e.userId()));
        if(event instanceof ExperienceLifecycleChangedEvent e)return Optional.of(experience(e.experienceId(),e.userId()));
        if(event instanceof ExperienceModerationDecidedEvent e)return Optional.of(experience(e.experienceId(),e.authorId()));
        if(event instanceof ExperienceMediaChangedEvent e)return Optional.of(new OutboxEventMetadata("ExperienceMedia",e.mediaId().toString(),"experience:"+e.experienceId()+":media"));
        if(event instanceof ExperienceReportedEvent e)return Optional.of(new OutboxEventMetadata("ExperienceReport",e.reportId().toString(),"user:"+e.reporterId()));
        if(event instanceof ExperienceAccountDataErasedEvent e)return Optional.of(new OutboxEventMetadata("ExperienceAccountDeletion",e.userId().toString(),"accountDeletion:"+e.userId()));
        return Optional.empty();
    }
    private OutboxEventMetadata experience(java.util.UUID id,java.util.UUID userId){return new OutboxEventMetadata("Experience",id.toString(),"user:"+userId+":experience");}
}
