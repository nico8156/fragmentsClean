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
  AvatarMedia available(){var item=AvatarMedia.pending(UUID.randomUUID(),owner,"image/jpeg",1024,"pending",at);item.confirm("avatar/"+item.id(),"image/jpeg",1024,512,512,"hash",at);media.save(item);return item;}
  ChangeAvatarMediaLifecycleCommand command(AvatarMedia m,String status,String reason){return new ChangeAvatarMediaLifecycleCommand(UUID.randomUUID(),m.id(),operator,status,reason);}
  @Test void success_preserves_profile_and_publishes_auditable_fact(){var item=available();handler.execute(command(item,"RETIRED"," Ancien fichier "));assertThat(media.inspect(item.id()).orElseThrow().status()).isEqualTo(AvatarMediaStatus.RETIRED);assertThat(events).hasSize(1);assertThat(audit.reason).isEqualTo("Ancien fichier");assertThat(audit.actor).isEqualTo(operator);assertThat(users.saves).isZero();}
  @Test void used_avatar_and_blank_reason_never_publish_or_audit(){var item=available();media.profileUsages.put(item.snapshot().objectKey(),1L);assertThatThrownBy(()->handler.execute(command(item,"RETIRED","Unused"))).hasMessageContaining("referenced");assertThatThrownBy(()->handler.execute(command(item,"RETIRED"," "))).hasMessageContaining("reason");assertThat(media.inspect(item.id()).orElseThrow().status()).isEqualTo(AvatarMediaStatus.AVAILABLE);assertThat(events).isEmpty();assertThat(audit.actor).isNull();}
  @Test void inactive_owner_prevents_restore_and_retirement(){var item=available();users.user.requestAccountDeletion(UUID.randomUUID(),at);assertThatThrownBy(()->handler.execute(command(item,"RETIRED","Unused"))).hasMessageContaining("not active");assertThat(events).isEmpty();assertThat(audit.actor).isNull();}
  @Test void missing_upload_is_a_business_rejection(){assertThatThrownBy(()->handler.execute(new ChangeAvatarMediaLifecycleCommand(UUID.randomUUID(),UUID.randomUUID(),operator,"RETIRED","Unused"))).hasMessageContaining("not found");assertThat(events).isEmpty();}
  static class FakeUsers implements AppUserRepository {
    AppUser user;int saves;
    public Optional<AppUser> findById(UUID id){return Optional.ofNullable(user).filter(u->u.id().equals(id));}
    public Optional<AppUser> findByAuthUserId(UUID id){return findById(id);}
    public AppUser save(AppUser value){saves++;user=value;return value;}
  }
  static class FakeAudit implements AdminAuditRecorder {
    UUID actor;String reason;
    public void record(UUID a,String b,String c,UUID d,UUID e,String f,Instant g){}
    public void recordDecision(UUID a,String b,String c,UUID d,UUID e,String f,String r,Instant g){actor=a;reason=r;}
  }
}
