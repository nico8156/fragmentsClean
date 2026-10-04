package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.eventing;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMediaChangedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.util.Optional;
import org.springframework.stereotype.Component;
@Component
public final class AvatarMediaOutboxMetadataContributor implements OutboxEventMetadataContributor {
    public Optional<OutboxEventMetadata> resolve(DomainEvent event){
        if(event instanceof AvatarMediaChangedEvent e)return Optional.of(new OutboxEventMetadata("AvatarMedia",e.mediaId().toString(),"avatarMedia:"+e.mediaId()));
        return Optional.empty();
    }
}
