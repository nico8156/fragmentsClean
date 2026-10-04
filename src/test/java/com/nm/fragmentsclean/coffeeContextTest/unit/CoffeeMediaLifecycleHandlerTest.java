package com.nm.fragmentsclean.coffeeContextTest.unit;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import java.util.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class CoffeeMediaLifecycleHandlerTest {
 @Test void retirement_preserves_photo_for_restoration_and_publishes_existing_event(){
  var coffees=new FakeCoffeeRepository();var memory=new FakeCoffeePhotoRetirementRepository();var events=new FakeDomainEventPublisher();Instant at=Instant.parse("2026-10-04T10:00:00Z");UUID id=UUID.randomUUID(),photoId=UUID.randomUUID(),actor=UUID.randomUUID();
  new CreateCoffeeCommandHandler(coffees,new FakeDomainEventPublisher(),()->at).execute(new CreateCoffeeCommand(UUID.randomUUID(),id,null,"Lifecycle","Street","Paris","75000","FR",0,0,null,null,List.of(),at));
  var coffee=coffees.findById(new CoffeeId(id)).orElseThrow();coffee.addPhoto(new Photo(new PhotoId(photoId),new CoffeeId(id),"s3://bucket/coffee.jpg",true,0),at);coffees.save(coffee);
  var audit=new RecordingAudit();var handler=new ChangeCoffeeMediaLifecycleCommandHandler(coffees,memory,events,audit,()->at);
  handler.execute(new ChangeCoffeeMediaLifecycleCommand(UUID.randomUUID(),id,photoId,actor,"RETIRED","Obsolete"));
  assertThat(coffees.findById(new CoffeeId(id)).orElseThrow().photos()).isEmpty();
  assertThat(memory.byId(photoId).orElseThrow().photo().uri()).isEqualTo("s3://bucket/coffee.jpg");
  assertThat(events.published.getFirst()).isInstanceOf(CoffeePhotoDeletedEvent.class);
  assertThat(events.published).hasSize(2);
  var inventory=(CoffeeMediaCatalogSnapshotEvent)events.published.get(1);
  assertThat(inventory.retiredPhotos()).containsExactly(new CoffeeMediaCatalogSnapshotEvent.RetiredPhoto(photoId,at));
  assertThat(inventory.photos()).isEmpty();assertThat(inventory.version()).isEqualTo(coffees.findById(new CoffeeId(id)).orElseThrow().version());
  assertThat(audit.decisions).containsExactly("COFFEE_MEDIA_RETIRED:Obsolete:"+actor);
  handler.execute(new ChangeCoffeeMediaLifecycleCommand(UUID.randomUUID(),id,photoId,actor,"AVAILABLE","Restore"));
  assertThat(events.published).hasSize(4);
  var restored=(CoffeeMediaCatalogSnapshotEvent)events.published.get(3);
  assertThat(restored.retiredPhotos()).isEmpty();assertThat(restored.photos()).extracting(CoffeePhotosArrangedEvent.ArrangedPhoto::photoId).containsExactly(photoId);
  handler.execute(new ChangeCoffeeMediaLifecycleCommand(UUID.randomUUID(),id,photoId,actor,"AVAILABLE","Already restored"));
  assertThat(events.published).hasSize(4);

 }
 static class RecordingAudit implements com.nm.fragmentsclean.sharedKernel.businesslogic.models.AdminAuditRecorder {
  final List<String> decisions=new ArrayList<>();
  public void record(UUID actor,String action,String target,UUID id,UUID command,String outcome,Instant at){}
  public void recordDecision(UUID actor,String action,String target,UUID id,UUID command,String outcome,String reason,Instant at){decisions.add(action+":"+reason+":"+actor);}
 }
}
