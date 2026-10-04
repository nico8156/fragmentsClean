package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoRetirementRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Component @Transactional public class ChangeCoffeeMediaLifecycleCommandHandler implements CommandHandler<ChangeCoffeeMediaLifecycleCommand> {
    private final com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage storage;
    private final CoffeeRepository coffees;
    private final CoffeePhotoRetirementRepository retirements;
    private final DomainEventPublisher events;
    private final AdminAuditRecorder audit;
    private final DateTimeProvider clock;
    public ChangeCoffeeMediaLifecycleCommandHandler(CoffeeRepository coffees,CoffeePhotoRetirementRepository retirements,DomainEventPublisher events,AdminAuditRecorder audit,DateTimeProvider clock,com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage storage){this.coffees=coffees;this.retirements=retirements;this.events=events;this.audit=audit;this.clock=clock;this.storage=storage;}
    public void execute(ChangeCoffeeMediaLifecycleCommand command){
        if(command.reason()==null || command.reason().isBlank() || command.reason().strip().length()>240)throw reject("MEDIA_REASON_REQUIRED");
        if(!"RETIRED".equals(command.status()) && !"AVAILABLE".equals(command.status()) && !"PURGE_REQUESTED".equals(command.status()))throw reject("MEDIA_STATUS_INVALID");
        var coffee=coffees.findById(new CoffeeId(command.coffeeId())).orElseThrow(()->reject("COFFEE_NOT_FOUND"));
        if(coffee.publicationStatus()==CoffeePublicationStatus.ARCHIVED)throw reject("COFFEE_ARCHIVED");
        var saved=retirements.byId(command.mediaId());
        if(saved.isPresent() && !saved.get().photo().coffeeId().equals(coffee.coffeeId()))throw reject("MEDIA_OWNERSHIP_MISMATCH");
        if(retirements.currentPhotoCount(command.mediaId())>1)throw reject("MEDIA_IDENTITY_AMBIGUOUS");
        var current=coffee.photos().stream().filter(p->p.id().value().equals(command.mediaId())).findFirst();
        var now=clock.now();boolean changed=false;boolean retiring="RETIRED".equals(command.status());
        if("PURGE_REQUESTED".equals(command.status())){
            var retired=saved.orElseThrow(()->reject("MEDIA_NOT_TRACKED"));
            if(!storage.canDeletePhoto(coffee.coffeeId(),command.mediaId(),retired.photo().uri()))throw reject("MEDIA_UNMANAGED_REFERENCE");
            var next=retired.requestPurge(now,retirements.referencesTo(command.mediaId(),storage.referencesForPhoto(coffee.coffeeId(),command.mediaId(),retired.photo().uri()))>0,command.commandId());
            if(next!=retired){retirements.save(next);coffee.recordMediaLifecycleChange(now);coffees.save(coffee);events.publish(CoffeeMediaCatalogSnapshotEvent.from(coffee,retirements.byCoffee(command.coffeeId()),command.commandId(),now));}
            audit.recordDecision(command.operatorId(),"COFFEE_MEDIA_PURGE_REQUESTED","COFFEE_MEDIA",command.mediaId(),command.commandId(),"APPLIED",command.reason().strip(),now);return;
        }
        if(saved.isPresent()&&!"RETIRED".equals(saved.get().status()))throw reject("MEDIA_STATE_INVALID");
        if(retiring){
            if(current.isPresent()){
                if(saved.isPresent())throw reject("MEDIA_SOURCE_CONFLICT");
                retirements.save(new CoffeePhotoRetirement(current.get(),now));
                coffee.removePhoto(current.get().id(),now);coffees.save(coffee);
                events.publish(new CoffeePhotoDeletedEvent(UUID.randomUUID(),command.commandId(),coffee.coffeeId(),current.get().id(),coffee.version(),now,null));changed=true;
            }else if(saved.isEmpty())throw reject("MEDIA_NOT_TRACKED");
        }else{
            if(current.isEmpty()){
                if(retirements.currentPhotoCount(command.mediaId())>0)throw reject("MEDIA_IDENTITY_AMBIGUOUS");
                var retired=saved.orElseThrow(()->reject("MEDIA_NOT_TRACKED"));
                coffee.addPhoto(retired.photo(),now);coffees.save(coffee);retirements.remove(command.mediaId());
                var photo=coffee.photos().stream().filter(p->p.id().value().equals(command.mediaId())).findFirst().orElseThrow();
                events.publish(new CoffeePhotoAddedEvent(UUID.randomUUID(),command.commandId(),coffee.coffeeId(),new ImportedCoffeePhoto(photo.id().value(),photo.uri()),photo.isCover(),photo.sortOrder(),coffee.version(),now,null));changed=true;
            }else if(saved.isPresent())throw reject("MEDIA_SOURCE_CONFLICT");
        }
        if(changed)events.publish(CoffeeMediaCatalogSnapshotEvent.from(coffee,retirements.byCoffee(command.coffeeId()),command.commandId(),now));
        audit.recordDecision(command.operatorId(),retiring?"COFFEE_MEDIA_RETIRED":"COFFEE_MEDIA_RESTORED","COFFEE_MEDIA",command.mediaId(),command.commandId(),"APPLIED",command.reason().strip(),now);
    }
    private static BusinessCommandRejectedException reject(String reason){return new BusinessCommandRejectedException(reason,"Coffee media lifecycle decision rejected");}
}
