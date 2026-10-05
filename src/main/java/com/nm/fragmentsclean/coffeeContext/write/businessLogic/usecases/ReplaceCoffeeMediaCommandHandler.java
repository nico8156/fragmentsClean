package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;
import java.util.*;
import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
@Component @Transactional public class ReplaceCoffeeMediaCommandHandler implements CommandHandler<ReplaceCoffeeMediaCommand> {
 private final CoffeeRepository coffees;private final CoffeePhotoRetirementRepository retirements;private final CoffeePhotoStorage storage;private final DomainEventPublisher events;private final AdminAuditRecorder audit;private final DateTimeProvider clock;
 public ReplaceCoffeeMediaCommandHandler(CoffeeRepository coffees,CoffeePhotoRetirementRepository retirements,CoffeePhotoStorage storage,DomainEventPublisher events,AdminAuditRecorder audit,DateTimeProvider clock){this.coffees=coffees;this.retirements=retirements;this.storage=storage;this.events=events;this.audit=audit;this.clock=clock;}
 public void execute(ReplaceCoffeeMediaCommand command){
  if(command.reason()==null||command.reason().isBlank()||command.reason().strip().length()>240)throw reject("MEDIA_REASON_REQUIRED");
  if(command.bytes().length==0||command.contentType()==null||!Set.of("image/jpeg","image/png","image/webp","image/gif").contains(command.contentType()))throw reject("MEDIA_IMAGE_REQUIRED");
  var coffee=coffees.findById(new CoffeeId(command.coffeeId())).orElseThrow(()->reject("COFFEE_NOT_FOUND"));
  if(coffee.isArchived())throw reject("COFFEE_ARCHIVED");if(retirements.currentPhotoCount(command.mediaId())>1)throw reject("MEDIA_IDENTITY_AMBIGUOUS");
  if(retirements.byId(command.mediaId()).isPresent())throw reject("MEDIA_STATE_INVALID");
  var previous=coffee.photos().stream().filter(p->p.id().value().equals(command.mediaId())).findFirst().orElseThrow(()->reject("MEDIA_NOT_TRACKED"));
  var sourceName="admin-upload/"+command.commandId()+"/"+command.fileName();var expected=CoffeePhotoStorage.photoId(coffee.coffeeId(),sourceName);
  if(retirements.byId(expected).isPresent()||retirements.currentPhotoCount(expected)>0||coffee.photos().stream().anyMatch(p->p.id().value().equals(expected)))throw reject("MEDIA_STORAGE_IDENTITY_CONFLICT");
  var stored=storage.store(coffee.coffeeId(),coffee.googleId().orElse(null),new GooglePlacePhoto(sourceName,command.contentType(),command.bytes()));
  if(!expected.equals(stored.photoId()))throw reject("MEDIA_STORAGE_IDENTITY_MISMATCH");
  var now=clock.now();coffee.replacePhoto(previous.id(),new Photo(new PhotoId(stored.photoId()),coffee.coffeeId(),stored.photoUri(),previous.isCover(),previous.sortOrder()),now);retirements.save(new CoffeePhotoRetirement(previous,now));coffees.save(coffee);
  events.publish(new CoffeePhotosArrangedEvent(UUID.randomUUID(),command.commandId(),coffee.coffeeId(),coffee.photos().stream().map(p->new CoffeePhotosArrangedEvent.ArrangedPhoto(p.id().value(),p.uri(),p.isCover(),p.sortOrder())).toList(),coffee.version(),now,null));
  events.publish(CoffeeMediaCatalogSnapshotEvent.from(coffee,retirements.byCoffee(command.coffeeId()),command.commandId(),now));
  audit.recordDecision(command.operatorId(),"COFFEE_MEDIA_REPLACED","COFFEE_MEDIA",command.mediaId(),command.commandId(),"APPLIED",command.reason().strip(),now);
  audit.recordDecision(command.operatorId(),"COFFEE_MEDIA_REPLACEMENT_ADDED","COFFEE_MEDIA",stored.photoId(),command.commandId(),"APPLIED",command.reason().strip(),now);
 }
 private static BusinessCommandRejectedException reject(String reason){return new BusinessCommandRejectedException(reason,"Coffee media replacement rejected");}
}
