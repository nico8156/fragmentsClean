package com.nm.fragmentsclean.coffeeContext.read;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.springframework.transaction.annotation.Transactional;
public class CoffeePhotosImportedEventHandler implements EventHandler<CoffeePhotosImportedEvent> {
 private final CoffeePhotoProjectionRefresh refresh;
 public CoffeePhotosImportedEventHandler(CoffeePhotoProjectionRefresh refresh){this.refresh=refresh;}
 @Override @Transactional public void handle(CoffeePhotosImportedEvent event){refresh.refresh(event.coffeeId().value());}
 @Transactional public void handle(com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotosImportedIntegrationEvent event){refresh.refresh(event.coffeeId());}
}
