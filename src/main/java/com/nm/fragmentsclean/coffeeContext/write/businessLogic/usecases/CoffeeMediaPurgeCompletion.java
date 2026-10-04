package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;
import java.util.*;
import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
@Component public class CoffeeMediaPurgeCompletion {
 private final CoffeeRepository coffees;private final CoffeePhotoRetirementRepository retirements;private final CoffeePhotoStorage storage;private final DomainEventPublisher events;private final DateTimeProvider clock;
 public CoffeeMediaPurgeCompletion(CoffeeRepository coffees,CoffeePhotoRetirementRepository retirements,CoffeePhotoStorage storage,DomainEventPublisher events,DateTimeProvider clock){this.coffees=coffees;this.retirements=retirements;this.storage=storage;this.events=events;this.clock=clock;}
 @Transactional public Optional<CoffeePhotoRetirement> pending(UUID id){
  var owner=retirements.ownerOf(id);if(owner.isEmpty()||coffees.findById(new CoffeeId(owner.get())).isEmpty())return Optional.empty();
  return retirements.byId(id).filter(r->"DELETION_PENDING".equals(r.status())&&storage.canDeletePhoto(r.photo().coffeeId(),id,r.photo().uri())&&retirements.referencesTo(id,storage.referencesForPhoto(r.photo().coffeeId(),id,r.photo().uri()))==0);
 }
 @Transactional public void complete(UUID id){
  var owner=retirements.ownerOf(id);if(owner.isEmpty())return;var coffee=coffees.findById(new CoffeeId(owner.get())).orElse(null);if(coffee==null)return;
  var item=retirements.byId(id);if(item.isEmpty()||!"DELETION_PENDING".equals(item.get().status()))return;var retired=item.get();if(retirements.referencesTo(id,storage.referencesForPhoto(coffee.coffeeId(),id,retired.photo().uri()))>0)return;
  var now=clock.now();retirements.save(retired.completePurge(now));coffee.recordMediaLifecycleChange(now);coffees.save(coffee);events.publish(CoffeeMediaCatalogSnapshotEvent.from(coffee,retirements.byCoffee(owner.get()),retired.purgeCommandId(),now));
 }
}
