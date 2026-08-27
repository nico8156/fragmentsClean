package com.nm.fragmentsclean.coffeeContext.businessLogic.processManagers;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;

public class CoffeeDeletedMediaCleanupHandler implements EventHandler<CoffeeDeletedEvent> {
    private final CoffeePhotoStorage photoStorage;
    public CoffeeDeletedMediaCleanupHandler(CoffeePhotoStorage photoStorage) { this.photoStorage = photoStorage; }
    @Override public void handle(CoffeeDeletedEvent event) { photoStorage.deleteForCoffee(new CoffeeId(event.coffeeId().value())); }
}
