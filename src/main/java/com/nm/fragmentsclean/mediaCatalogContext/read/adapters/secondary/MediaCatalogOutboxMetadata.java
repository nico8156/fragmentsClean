package com.nm.fragmentsclean.mediaCatalogContext.read.adapters.secondary;
import com.nm.fragmentsclean.mediaCatalogContext.read.MediaCatalogAccountDataErasedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.*;
import java.util.Optional;
import org.springframework.stereotype.Component;
@Component
public final class MediaCatalogOutboxMetadata implements OutboxEventMetadataContributor {
    public Optional<OutboxEventMetadata> resolve(DomainEvent event){
        if(event instanceof MediaCatalogAccountDataErasedEvent e)return Optional.of(new OutboxEventMetadata("MediaCatalogAccountDeletion",e.userId().toString(),"accountDeletion:"+e.userId()));
        return Optional.empty();
    }
}
