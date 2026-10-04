package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosArrangedEvent.ArrangedPhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import java.time.Instant;
import java.util.*;
public interface CoffeeMediaCatalogScan {
    record Snapshot(CoffeeId coffeeId,List<ArrangedPhoto> photos,int version,Instant updatedAt) { public Snapshot {photos=List.copyOf(photos);} }
    record Batch(boolean due,List<Snapshot> items) { public Batch {items=List.copyOf(items);} }
    Batch lockNext(int limit);
    void advance(UUID cursor,boolean complete);
}
