package com.nm.fragmentsclean.coffeeContext.read;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** Photos follow the existing Coffee source-snapshot projection convention. */
@Service public class CoffeePhotoProjectionRefresh {
 private final CoffeePhotoProjectionSource source;
 private final CoffeePhotoProjectionRepository photos;
 private final CoffeePublicProjectionChangePolicy publicPolicy;
 private final ProjectionSyncPublisher sync;
 public CoffeePhotoProjectionRefresh(CoffeePhotoProjectionSource source,CoffeePhotoProjectionRepository photos,CoffeeProjectionRepository coffees,ProjectionSyncPublisher sync){this.source=source;this.photos=photos;this.publicPolicy=new CoffeePublicProjectionChangePolicy(coffees);this.sync=sync;}
 @Transactional public void refresh(UUID id){
  var current=source.current(id);
  if(current.isEmpty()){photos.deleteForCoffee(id);return;}
  var snapshot=current.get();photos.replaceForCoffee(id,snapshot.photos());
  if(!"PUBLISHED".equals(snapshot.publicationStatus()) || !publicPolicy.isPubliclyVisible(id))return;
  sync.publish(ProjectionSyncEvent.publicProjectionUpdated("coffees","entity",id.toString(),snapshot.version(),snapshot.changedAt(),List.of("photos")));
 }
}
