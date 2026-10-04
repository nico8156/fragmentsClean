package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.*;
@Component public final class CleanCoffeeMediaObjects {
 private final CoffeePhotoRetirementRepository retirements;private final CoffeePhotoStorage storage;private final CoffeeMediaPurgeCompletion completion;
 public CleanCoffeeMediaObjects(CoffeePhotoRetirementRepository retirements,CoffeePhotoStorage storage,CoffeeMediaPurgeCompletion completion){this.retirements=retirements;this.storage=storage;this.completion=completion;}
 public void run(int limit){for(var id:retirements.pendingPurgeIds(limit)){var item=completion.pending(id);if(item.isEmpty())continue;var retired=item.get();storage.deletePhoto(retired.photo().coffeeId(),id,retired.photo().uri());completion.complete(id);}}
}
