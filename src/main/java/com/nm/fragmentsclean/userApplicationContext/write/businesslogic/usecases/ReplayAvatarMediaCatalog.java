package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AvatarMediaCatalogScan;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMediaChangedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReplayAvatarMediaCatalog {
    private final AvatarMediaCatalogScan scan;private final DomainEventPublisher events;
    public ReplayAvatarMediaCatalog(AvatarMediaCatalogScan scan,DomainEventPublisher events){this.scan=scan;this.events=events;}
    @Transactional public int nextBatch(){
        var batch=scan.lockNext(100);if(!batch.due())return 0;
        for(var item:batch.items())events.publish(AvatarMediaChangedEvent.from(item.media(),UUID.randomUUID(),item.profileUserId()));
        scan.advance(batch.items().isEmpty()?null:batch.items().getLast().media().mediaId(),batch.items().size()<100);
        return batch.items().size();
    }
}
