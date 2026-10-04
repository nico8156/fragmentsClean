package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeeMediaCatalogScan;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeMediaCatalogSnapshotEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReplayCoffeeMediaCatalog {
    private final CoffeeMediaCatalogScan scan;private final DomainEventPublisher events;
    public ReplayCoffeeMediaCatalog(CoffeeMediaCatalogScan scan,DomainEventPublisher events){this.scan=scan;this.events=events;}
    @Transactional public int nextBatch(){
        var batch=scan.lockNext(100);if(!batch.due())return 0;
        for(var s:batch.items())events.publish(new CoffeeMediaCatalogSnapshotEvent(UUID.randomUUID(),UUID.randomUUID(),s.coffeeId(),s.photos(),s.version(),s.updatedAt(),null));
        scan.advance(batch.items().isEmpty()?null:batch.items().getLast().coffeeId().value(),batch.items().size()<100);
        return batch.items().size();
    }
}
