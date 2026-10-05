package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaCatalogScan;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaChangedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReplayExperienceMediaCatalog {
    private final ExperienceMediaCatalogScan scan;
    private final DomainEventPublisher events;
    public ReplayExperienceMediaCatalog(ExperienceMediaCatalogScan scan,DomainEventPublisher events){this.scan=scan;this.events=events;}
    @Transactional public int nextBatch() {
        var batch=scan.lockNext(100);
        if(!batch.due())return 0;
        for(var s:batch.items())events.publish(new ExperienceMediaChangedEvent(UUID.randomUUID(),UUID.randomUUID(),s.mediaId(),s.experienceId(),s.coffeeId(),s.userId(),s.status(),s.objectKey(),s.contentType(),s.size(),s.width(),s.height(),"CATALOG_RECONCILIATION",s.version(),s.updatedAt(),null,s.createdAt()));
        scan.advance(batch.items().isEmpty()?null:batch.items().getLast().mediaId(),batch.items().size()<100);
        return batch.items().size();
    }
}
