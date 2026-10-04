package com.nm.fragmentsclean.userApplicationContextTest.unit;
import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.fake.FakeAvatarMediaRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import java.util.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
class AvatarMediaLifecycleHandlerTest {
  final Instant at=Instant.parse("2026-10-04T10:00:00Z");
  final UUID owner=UUID.randomUUID(),operator=UUID.randomUUID();
  final FakeUsers users=new FakeUsers();
  final FakeAvatarMediaRepository media=new FakeAvatarMediaRepository();
  final List<DomainEvent> events=new ArrayList<>();
  final FakeAudit audit=new FakeAudit();
  final ChangeAvatarMediaLifecycleCommandHandler handler=new ChangeAvatarMediaLifecycleCommandHandler(users,media,events::add,audit,()->at);
  AvatarMediaLifecycleHandlerTest(){users.user=new AppUser(owner,owner,"Owner",null,at,at,0);}
  AvatarMedia available(){return available(at);}
  AvatarMedia available(Instant created){var item=AvatarMedia.pending(UUID.randomUUID(),owner,"image/jpeg",1024,"pending",created);item.confirm("avatar/"+item.id(),"image/jpeg",1024,512,512,"hash",created);media.save(item);return item;}
  ChangeAvatarMediaLifecycleCommand command(AvatarMedia m,String status,String reason){return new ChangeAvatarMediaLifecycleCommand(UUID.randomUUID(),m.id(),operator,status,reason);}
  @Test void an_unused_avatar_retired_for_thirty_full_days_can_explicitly_request_purge(){
    var item=available(at.minus(java.time.Duration.ofDays(30)));item.retire(false,at.minus(java.time.Duration.ofDays(30)));media.save(item);
    assertThatNoException().isThrownBy(()->handler.execute(command(item,"PURGE_REQUESTED","Rétention terminée")));
    var source=media.inspect(item.id()).orElseThrow();
    assertThat(source.status()).isEqualTo(AvatarMediaStatus.DELETION_PENDING);
    assertThat(source.objectKey()).isEqualTo(item.snapshot().objectKey());assertThat(source.userId()).isEqualTo(owner);
    assertThat(events).hasSize(1);assertThat(audit.action).isEqualTo("AVATAR_MEDIA_PURGE_REQUESTED");assertThat(audit.actor).isEqualTo(operator);
  }
  @Test void a_millisecond_before_thirty_days_retention_still_blocks_purge(){
    var retiredAt=at.minus(java.time.Duration.ofDays(30)).plusMillis(1);var item=available(retiredAt);item.retire(false,retiredAt);media.save(item);
    assertThatThrownBy(()->handler.execute(command(item,"PURGE_REQUESTED","Too early")))
      .isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class)
      .extracting(e->((com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException)e).rejectionCode()).isEqualTo("MEDIA_RETENTION_ACTIVE");
    assertThat(media.inspect(item.id()).orElseThrow().status()).isEqualTo(AvatarMediaStatus.RETIRED);assertThat(events).isEmpty();assertThat(audit.actor).isNull();
  }
  @Test void an_old_retired_avatar_still_used_by_a_profile_cannot_be_purged(){
    var retiredAt=at.minus(java.time.Duration.ofDays(31));var item=available(retiredAt);item.retire(false,retiredAt);media.save(item);media.profileUsages.put(item.snapshot().objectKey(),1L);
    assertThatThrownBy(()->handler.execute(command(item,"PURGE_REQUESTED","Unused")))
      .isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class)
      .extracting(e->((com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException)e).rejectionCode()).isEqualTo("MEDIA_IN_USE");
    assertThat(events).isEmpty();assertThat(audit.actor).isNull();
  }
  @Test void age_alone_does_not_delete_and_storage_failure_does_not_invent_physical_completion(){
    var retiredAt=at.minus(java.time.Duration.ofDays(31));var item=available(retiredAt);item.retire(false,retiredAt);media.save(item);
    var store=new RecordingStorage();var cleaner=new CleanAvatarMediaObjects(media,store,new CompleteAvatarMediaDeletion(media,()->at,events::add),()->at,java.time.Duration.ofDays(1));
    cleaner.run(100);assertThat(store.deleted).isEmpty();assertThat(media.inspect(item.id()).orElseThrow().status()).isEqualTo(AvatarMediaStatus.RETIRED);
    handler.execute(command(item,"PURGE_REQUESTED","Rétention terminée"));
    store.failOn=item.snapshot().objectKey();assertThatThrownBy(()->cleaner.run(100)).hasMessageContaining("Storage unavailable");
    assertThat(media.inspect(item.id()).orElseThrow().status()).isEqualTo(AvatarMediaStatus.DELETION_PENDING);assertThat(events).hasSize(1);
    store.failOn=null;cleaner.run(100);
    assertThat(store.deleted).containsExactly("pending",item.snapshot().objectKey(),"pending",item.snapshot().objectKey());
    assertThat(media.inspect(item.id()).orElseThrow().status()).isEqualTo(AvatarMediaStatus.DELETED);assertThat(events).hasSize(2);
    assertThat(((AvatarMediaChangedEvent)events.getLast()).status()).isEqualTo("DELETED");
  }
  @Test void success_preserves_profile_and_publishes_auditable_fact(){var item=available();handler.execute(command(item,"RETIRED"," Ancien fichier "));assertThat(media.inspect(item.id()).orElseThrow().status()).isEqualTo(AvatarMediaStatus.RETIRED);assertThat(events).hasSize(1);assertThat(audit.reason).isEqualTo("Ancien fichier");assertThat(audit.actor).isEqualTo(operator);assertThat(users.saves).isZero();}
  @Test void used_avatar_and_blank_reason_never_publish_or_audit(){var item=available();media.profileUsages.put(item.snapshot().objectKey(),1L);assertThatThrownBy(()->handler.execute(command(item,"RETIRED","Unused"))).hasMessageContaining("referenced");assertThatThrownBy(()->handler.execute(command(item,"RETIRED"," "))).hasMessageContaining("reason");assertThat(media.inspect(item.id()).orElseThrow().status()).isEqualTo(AvatarMediaStatus.AVAILABLE);assertThat(events).isEmpty();assertThat(audit.actor).isNull();}
  @Test void inactive_owner_prevents_restore_and_retirement(){var item=available();users.user.requestAccountDeletion(UUID.randomUUID(),at);assertThatThrownBy(()->handler.execute(command(item,"RETIRED","Unused"))).hasMessageContaining("not active");assertThat(events).isEmpty();assertThat(audit.actor).isNull();}
  @Test void missing_upload_is_a_business_rejection(){assertThatThrownBy(()->handler.execute(new ChangeAvatarMediaLifecycleCommand(UUID.randomUUID(),UUID.randomUUID(),operator,"RETIRED","Unused"))).hasMessageContaining("not found");assertThat(events).isEmpty();}
  static class RecordingStorage implements com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore {
    final List<String> deleted=new ArrayList<>();String failOn;
    public void delete(String key){deleted.add(key);if(Objects.equals(key,failOn))throw new IllegalStateException("Storage unavailable");}
    public UploadTarget presignUpload(String key,String type,java.time.Duration ttl,Instant now){throw new UnsupportedOperationException();}
    public ProcessedImage normalize(String pending,String target,String type,ImageRules rules){throw new UnsupportedOperationException();}
    public java.net.URI presignDownload(String key,java.time.Duration ttl){throw new UnsupportedOperationException();}
  }
  static class FakeUsers implements AppUserRepository {
    AppUser user;int saves;
    public Optional<AppUser> findById(UUID id){return Optional.ofNullable(user).filter(u->u.id().equals(id));}
    public Optional<AppUser> findByAuthUserId(UUID id){return findById(id);}
    public AppUser save(AppUser value){saves++;user=value;return value;}
  }
  static class FakeAudit implements AdminAuditRecorder {
    UUID actor;String reason;String action;
    public void record(UUID a,String b,String c,UUID d,UUID e,String f,Instant g){}
    public void recordDecision(UUID a,String b,String c,UUID d,UUID e,String f,String r,Instant g){actor=a;reason=r;action=b;}
  }
}
