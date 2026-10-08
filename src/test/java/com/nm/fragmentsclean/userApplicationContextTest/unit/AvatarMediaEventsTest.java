package com.nm.fragmentsclean.userApplicationContextTest.unit;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.fake.FakeAvatarMediaRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaObjectKeys;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class AvatarMediaEventsTest {
    final Instant at=Instant.parse("2026-10-04T10:00:00Z");
    final UUID user=UUID.randomUUID();
    final FakeAvatarMediaRepository media=new FakeAvatarMediaRepository();
    final FakeUsers users=new FakeUsers(new AppUser(user,user,"Ada",null,at,at,0));
    final FakeDomainEventPublisher events=new FakeDomainEventPublisher();
    @Test void pending_intent_is_published_once_with_its_real_creation_date(){
        var register=new RegisterAvatarUploadIntent(users,media,new PrivateMediaObjectKeys("private"),()->at,events);
        UUID id=UUID.randomUUID();register.register(id,user,"image/png",256);register.register(id,user,"image/png",256);
        assertThat(changes()).hasSize(1);
        var event=changes().getFirst();assertThat(event.status()).isEqualTo("PENDING");assertThat(event.createdAt()).isEqualTo(at);
        assertThat(event.profileUserId()).isNull();assertThat(event.userId()).isEqualTo(user);
    }
    @Test void confirmation_exposes_normalized_metadata_and_current_profile_usage(){
        UUID id=pending();confirm(id);
        assertThat(changes()).hasSize(1);var event=changes().getFirst();
        assertThat(event.status()).isEqualTo("AVAILABLE");assertThat(event.profileUserId()).isEqualTo(user);
        assertThat(event.width()).isEqualTo(512);assertThat(event.size()).isEqualTo(1024);assertThat(event.version()).isEqualTo(1);
        confirm(id);assertThat(changes()).hasSize(1);
    }
    @Test void replacement_publishes_old_retirement_and_new_availability(){
        UUID first=pending();confirm(first);events.published.clear();UUID second=pending();confirm(second);
        assertThat(changes()).hasSize(2);
        assertThat(changes()).anySatisfy(e->{assertThat(e.mediaId()).isEqualTo(first);assertThat(e.status()).isEqualTo("DELETION_PENDING");assertThat(e.profileUserId()).isNull();});
        assertThat(changes()).anySatisfy(e->{assertThat(e.mediaId()).isEqualTo(second);assertThat(e.status()).isEqualTo("AVAILABLE");assertThat(e.profileUserId()).isEqualTo(user);});
    }
    @Test void removal_retires_the_avatar_and_does_not_repeat_the_fact(){
        UUID id=pending();confirm(id);events.published.clear();var handler=new RemoveAvatarCommandHandler(users,media,events,()->at);
        handler.execute(new RemoveAvatarCommand(UUID.randomUUID(),user,at));handler.execute(new RemoveAvatarCommand(UUID.randomUUID(),user,at));
        assertThat(changes()).hasSize(1);assertThat(changes().getFirst().status()).isEqualTo("DELETION_PENDING");assertThat(changes().getFirst().profileUserId()).isNull();
    }
    @Test void deletion_fact_scrubs_the_storage_key_and_owner_even_when_the_source_keeps_cleanup_metadata(){
        UUID id=pending();confirm(id);var item=media.byId(id).orElseThrow();item.requestDeletion(at);media.save(item);events.published.clear();
        var complete=new CompleteAvatarMediaDeletion(media,()->at,events);complete.complete(id);complete.complete(id);
        assertThat(changes()).hasSize(1);var event=changes().getFirst();
        assertThat(event.status()).isEqualTo("DELETED");assertThat(event.userId()).isNull();assertThat(event.profileUserId()).isNull();
        assertThat(event.objectKey()).isNull();assertThat(event.contentType()).isNull();assertThat(event.size()).isZero();assertThat(event.width()).isNull();
    }
    @Test void flagged_avatar_keeps_old_profile_until_human_approval(){
        UUID old=pending();confirm(old);String oldUrl=users.value.avatarUrl();
        UUID candidate=pending();flag(candidate);
        assertThat(users.value.avatarUrl()).isEqualTo(oldUrl);
        assertThat(media.byId(candidate).orElseThrow().snapshot().status()).isEqualTo(AvatarMediaStatus.REVIEW_REQUIRED);
        review(candidate,true);
        assertThat(media.byId(candidate).orElseThrow().snapshot().status()).isEqualTo(AvatarMediaStatus.AVAILABLE);
        assertThat(media.byId(old).orElseThrow().snapshot().status()).isEqualTo(AvatarMediaStatus.DELETION_PENDING);
        assertThat(users.value.avatarUrl()).isNotEqualTo(oldUrl);
        assertThat(changes()).anySatisfy(e->{assertThat(e.mediaId()).isEqualTo(candidate);assertThat(e.profileUserId()).isEqualTo(user);});
    }
    @Test void refusal_preserves_the_previous_avatar(){
        UUID old=pending();confirm(old);String oldUrl=users.value.avatarUrl();UUID candidate=pending();flag(candidate);
        review(candidate,false);
        assertThat(users.value.avatarUrl()).isEqualTo(oldUrl);
        assertThat(media.byId(old).orElseThrow().snapshot().status()).isEqualTo(AvatarMediaStatus.AVAILABLE);
        assertThat(media.byId(candidate).orElseThrow().snapshot().status()).isEqualTo(AvatarMediaStatus.REJECTED);
    }
    @Test void removal_cancels_review_even_without_an_active_avatar(){
        UUID candidate=pending();flag(candidate);
        new RemoveAvatarCommandHandler(users,media,events,()->at).execute(new RemoveAvatarCommand(UUID.randomUUID(),user,at));
        assertThat(media.byId(candidate).orElseThrow().snapshot().status()).isEqualTo(AvatarMediaStatus.DELETION_PENDING);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->review(candidate,true)).isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class);
        assertThat(users.value.avatarUrl()).isNull();
    }
    @Test void an_older_review_cannot_replace_a_newer_active_avatar(){
        UUID candidate=pending();flag(candidate);
        UUID newer=UUID.randomUUID();media.save(AvatarMedia.pending(newer,user,"image/png",256,"pending/"+newer,at.plusSeconds(1)));confirm(newer);
        String current=users.value.avatarUrl();
        org.assertj.core.api.Assertions.assertThatThrownBy(()->review(candidate,true)).isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class);
        assertThat(users.value.avatarUrl()).isEqualTo(current);
    }
    private void flag(UUID id){new ConfirmAvatarCommandHandler(users,media,events,()->at).executeReviewed(new ConfirmAvatarCommand(UUID.randomUUID(),id,user,"avatars/"+id,"image/jpeg",1024,512,512,"hash",at),true);}
    private void review(UUID id,boolean approve){new ReviewAvatarMediaCommandHandler(users,media,events,(a,b,c,d,e,f,g)->{},()->at).execute(new ReviewAvatarMediaCommand(UUID.randomUUID(),id,user,approve,"Décision opérateur"));}

    private UUID pending(){UUID id=UUID.randomUUID();media.save(AvatarMedia.pending(id,user,"image/png",256,"pending/"+id,at));return id;}
    private void confirm(UUID id){new ConfirmAvatarCommandHandler(users,media,events,()->at).execute(new ConfirmAvatarCommand(UUID.randomUUID(),id,user,"avatars/"+id,"image/jpeg",1024,512,512,"hash",at));}
    private List<AvatarMediaChangedEvent> changes(){return events.published.stream().filter(AvatarMediaChangedEvent.class::isInstance).map(AvatarMediaChangedEvent.class::cast).toList();}
    private static final class FakeUsers implements AppUserRepository {
        private AppUser value;FakeUsers(AppUser value){this.value=value;}
        public Optional<AppUser> findByAuthUserId(UUID id){return findById(id);}
        public Optional<AppUser> findById(UUID id){return id.equals(value.id())?Optional.of(value):Optional.empty();}
        public AppUser save(AppUser user){value=user;return user;}
    }
}
