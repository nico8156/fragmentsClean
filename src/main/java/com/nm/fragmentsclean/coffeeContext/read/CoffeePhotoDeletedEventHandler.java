package com.nm.fragmentsclean.coffeeContext.read;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.springframework.transaction.annotation.Transactional;
public class CoffeePhotoDeletedEventHandler implements EventHandler<CoffeePhotoDeletedEvent> {
 private final CoffeePhotoProjectionRefresh refresh;
 public CoffeePhotoDeletedEventHandler(CoffeePhotoProjectionRefresh refresh){this.refresh=refresh;}
 @Override @Transactional public void handle(CoffeePhotoDeletedEvent event){refresh.refresh(event.coffeeId().value());}

}
