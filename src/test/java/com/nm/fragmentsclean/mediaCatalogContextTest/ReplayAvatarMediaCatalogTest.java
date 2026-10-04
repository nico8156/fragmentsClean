package com.nm.fragmentsclean.mediaCatalogContextTest;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AvatarMediaCatalogScan;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.ReplayAvatarMediaCatalog;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class ReplayAvatarMediaCatalogTest {
    @Test void replay_keeps_upload_date_version_and_actual_profile_usage(){
        var scan=new FakeAvatarScan();var events=new ArrayList<DomainEvent>();
        UUID id=UUID.randomUUID(),owner=UUID.randomUUID();var at=Instant.parse("2026-10-04T10:00:00Z");
        var snapshot=AvatarMedia.pending(id,owner,"image/png",256,"pending/"+id,at).snapshot();
        scan.batch=new AvatarMediaCatalogScan.Batch(true,List.of(new AvatarMediaCatalogScan.Item(snapshot,null)));
        assertThat(new ReplayAvatarMediaCatalog(scan,events::add).nextBatch()).isEqualTo(1);
        var event=(AvatarMediaChangedEvent)events.getFirst();
        assertThat(event.mediaId()).isEqualTo(id);assertThat(event.userId()).isEqualTo(owner);
        assertThat(event.profileUserId()).isNull();assertThat(event.createdAt()).isEqualTo(at);
        assertThat(event.version()).isZero();assertThat(scan.limit).isEqualTo(100);
        assertThat(scan.advanced).isTrue();assertThat(scan.complete).isTrue();
    }
    @Test void publisher_failure_does_not_advance_checkpoint(){
        var scan=new FakeAvatarScan();var at=Instant.parse("2026-10-04T10:00:00Z");
        scan.batch=new AvatarMediaCatalogScan.Batch(true,List.of(new AvatarMediaCatalogScan.Item(AvatarMedia.pending(UUID.randomUUID(),UUID.randomUUID(),"image/jpeg",256,"pending/key",at).snapshot(),null)));
        org.assertj.core.api.Assertions.assertThatThrownBy(()->new ReplayAvatarMediaCatalog(scan,event->{throw new IllegalStateException("outbox unavailable");}).nextBatch()).isInstanceOf(IllegalStateException.class);
        assertThat(scan.advanced).isFalse();
    }
    @Test void scan_not_due_does_not_publish_or_advance(){
        var scan=new FakeAvatarScan();var events=new ArrayList<DomainEvent>();
        assertThat(new ReplayAvatarMediaCatalog(scan,events::add).nextBatch()).isZero();
        assertThat(events).isEmpty();assertThat(scan.advanced).isFalse();
    }
    private static final class FakeAvatarScan implements AvatarMediaCatalogScan {
        Batch batch=new Batch(false,List.of());int limit;boolean advanced,complete;
        public Batch lockNext(int limit){this.limit=limit;return batch;}
        public void advance(UUID id,boolean complete){this.advanced=true;this.complete=complete;}
    }
}
