package com.nm.fragmentsclean.coffeeContextTest.unit;
import java.time.*;import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.*;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
class CoffeeMediaPurgeTest {
 final Instant retired=Instant.parse("2026-09-01T00:00:00Z"),now=retired.plus(Duration.ofDays(30));final UUID coffeeId=UUID.randomUUID(),photoId=UUID.randomUUID(),command=UUID.randomUUID();
 CoffeePhotoRetirement item(){return new CoffeePhotoRetirement(new Photo(new PhotoId(photoId),new CoffeeId(coffeeId),"managed",false,0),retired);}
 @Test void full_thirty_days_and_no_reference_are_required(){
  assertThat(item().eligibleForPurge(now.minusNanos(1),false)).isFalse();assertThat(item().eligibleForPurge(now,false)).isTrue();
  assertThatThrownBy(()->item().requestPurge(now.minusNanos(1),false,command)).hasMessageContaining("MEDIA_RETENTION_ACTIVE");
  assertThatThrownBy(()->item().requestPurge(now,true,command)).hasMessageContaining("MEDIA_IN_USE");
 }
 @Test void requests_and_completion_are_idempotent_and_keep_original_decision(){
  var pending=item().requestPurge(now,false,command);assertThat(pending.status()).isEqualTo("DELETION_PENDING");assertThat(pending.requestPurge(now,false,UUID.randomUUID())).isSameAs(pending);
  var deleted=pending.completePurge(now.plusSeconds(1));assertThat(deleted.status()).isEqualTo("DELETED");assertThat(deleted.purgeCommandId()).isEqualTo(command);assertThat(deleted.retiredAt()).isEqualTo(retired);assertThat(deleted.completePurge(now)).isSameAs(deleted);
  assertThatThrownBy(()->deleted.requestPurge(now,false,command)).hasMessageContaining("MEDIA_STATE_INVALID");assertThatThrownBy(()->item().completePurge(now)).hasMessageContaining("MEDIA_STATE_INVALID");
 }
 @Test void cleaner_never_purges_on_age_alone_and_retries_storage_failure(){
  var memory=new FakeCoffeePhotoRetirementRepository();var coffees=new FakeCoffeeRepository();var events=new FakeDomainEventPublisher();var storage=new RecordingStorage();
  new CreateCoffeeCommandHandler(coffees,events,()->now).execute(new CreateCoffeeCommand(UUID.randomUUID(),coffeeId,null,"Purge","Street","Paris","75000","FR",0,0,null,null,List.of(),now));events.published.clear();memory.save(item());
  var completion=new CoffeeMediaPurgeCompletion(coffees,memory,storage,events,()->now);var cleaner=new CleanCoffeeMediaObjects(memory,storage,completion);
  cleaner.run(100);assertThat(storage.deleted).isEmpty();assertThat(memory.byId(photoId).orElseThrow().status()).isEqualTo("RETIRED");
  memory.save(item().requestPurge(now,false,command));storage.fail=true;assertThatThrownBy(()->cleaner.run(100)).hasMessageContaining("storage unavailable");assertThat(memory.byId(photoId).orElseThrow().status()).isEqualTo("DELETION_PENDING");assertThat(events.published).isEmpty();
  storage.fail=false;cleaner.run(100);assertThat(storage.deleted).containsExactly(photoId);assertThat(memory.byId(photoId).orElseThrow().status()).isEqualTo("DELETED");assertThat(events.published).hasSize(1);var snapshot=(CoffeeMediaCatalogSnapshotEvent)events.published.getFirst();assertThat(snapshot.retiredPhotos().getFirst().status()).isEqualTo("DELETED");assertThat(snapshot.commandId()).isEqualTo(command);cleaner.run(100);assertThat(storage.deleted).hasSize(1);
 }
 @Test void cleaner_rechecks_shared_references_before_physical_deletion(){
  var memory=new FakeCoffeePhotoRetirementRepository();var coffees=new FakeCoffeeRepository();var events=new FakeDomainEventPublisher();var storage=new RecordingStorage();new CreateCoffeeCommandHandler(coffees,events,()->now).execute(new CreateCoffeeCommand(UUID.randomUUID(),coffeeId,null,"Purge","Street","Paris","75000","FR",0,0,null,null,List.of(),now));memory.save(item().requestPurge(now,false,command));memory.referenceCounts.put("managed",1L);
  new CleanCoffeeMediaObjects(memory,storage,new CoffeeMediaPurgeCompletion(coffees,memory,storage,events,()->now)).run(100);assertThat(storage.deleted).isEmpty();assertThat(memory.byId(photoId).orElseThrow().status()).isEqualTo("DELETION_PENDING");
 }
 static class RecordingStorage implements CoffeePhotoStorage{boolean fail;final List<UUID>deleted=new ArrayList<>();public ImportedCoffeePhoto store(CoffeeId c,GooglePlaceId g,GooglePlacePhoto p){throw new UnsupportedOperationException();}public boolean canDeletePhoto(CoffeeId c,UUID id,String ref){return ref.equals("managed");}public void deletePhoto(CoffeeId c,UUID id,String ref){if(fail)throw new IllegalStateException("storage unavailable");deleted.add(id);}}
}
