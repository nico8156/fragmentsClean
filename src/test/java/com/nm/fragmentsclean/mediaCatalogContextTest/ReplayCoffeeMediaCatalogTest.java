package com.nm.fragmentsclean.mediaCatalogContextTest;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeeMediaCatalogScan;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.*;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ReplayCoffeeMediaCatalog;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class ReplayCoffeeMediaCatalogTest {
    @Test void replay_publishes_the_original_inventory_and_version_before_advancing_its_checkpoint(){
        var scan=new FakeCoffeeScan();var events=new ArrayList<DomainEvent>();
        var coffee=UUID.randomUUID();var at=Instant.parse("2026-10-04T10:00:00Z");
        var photos=List.of(new CoffeePhotosArrangedEvent.ArrangedPhoto(UUID.randomUUID(),"https://images.test/photo.jpg",true,0));
        scan.batch=new CoffeeMediaCatalogScan.Batch(true,List.of(new CoffeeMediaCatalogScan.Snapshot(new CoffeeId(coffee),photos,7,at)));
        var replay=new ReplayCoffeeMediaCatalog(scan,events::add);
        assertThat(replay.nextBatch()).isEqualTo(1);
        assertThat(events).hasSize(1);
        var event=(CoffeeMediaCatalogSnapshotEvent)events.getFirst();
        assertThat(event.coffeeId().value()).isEqualTo(coffee);assertThat(event.version()).isEqualTo(7);
        assertThat(event.occurredAt()).isEqualTo(at);assertThat(event.photos()).isEqualTo(photos);
        assertThat(scan.advanced).isTrue();assertThat(scan.complete).isTrue();
        assertThat(scan.limit).isEqualTo(100);
    }
    @Test void another_worker_or_a_scan_not_yet_due_does_not_publish_or_advance(){
        var scan=new FakeCoffeeScan();var events=new ArrayList<DomainEvent>();
        assertThat(new ReplayCoffeeMediaCatalog(scan,events::add).nextBatch()).isZero();
        assertThat(events).isEmpty();assertThat(scan.advanced).isFalse();
    }
    private static final class FakeCoffeeScan implements CoffeeMediaCatalogScan {
        Batch batch=new Batch(false,List.of());int limit;boolean advanced,complete;
        public Batch lockNext(int limit){this.limit=limit;return batch;}
        public void advance(UUID id,boolean complete){this.advanced=true;this.complete=complete;}
    }
}
