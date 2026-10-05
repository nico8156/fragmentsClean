package com.nm.fragmentsclean.coffeeContext.read;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.springframework.transaction.annotation.Transactional;
public class CoffeePhotoAddedEventHandler implements EventHandler<CoffeePhotoAddedEvent> {
 private final CoffeePhotoProjectionRefresh refresh;
 public CoffeePhotoAddedEventHandler(CoffeePhotoProjectionRefresh refresh){this.refresh=refresh;}
 @Override @Transactional public void handle(CoffeePhotoAddedEvent event){refresh.refresh(event.coffeeId().value());}
 @Transactional public void handle(com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoAddedIntegrationEvent event){refresh.refresh(event.coffeeId());}
}
