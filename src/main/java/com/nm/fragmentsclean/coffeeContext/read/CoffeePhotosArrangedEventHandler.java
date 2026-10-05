package com.nm.fragmentsclean.coffeeContext.read;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotosArrangedIntegrationEvent;
import org.springframework.transaction.annotation.Transactional;
public class CoffeePhotosArrangedEventHandler {
 private final CoffeePhotoProjectionRefresh refresh;
 public CoffeePhotosArrangedEventHandler(CoffeePhotoProjectionRefresh refresh){this.refresh=refresh;}
 @Transactional public void handle(CoffeePhotosArrangedIntegrationEvent event){refresh.refresh(event.coffeeId());}
}
