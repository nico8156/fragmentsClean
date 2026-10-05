package com.nm.fragmentsclean.articleContextTest.unit;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaCatalogSnapshotEvent;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaCatalogSnapshotEvent.Reference;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ArticleMediaCatalogPublisherTest {
    final Instant at=Instant.parse("2026-10-04T12:00:00Z");
    static class FakeSource implements ArticleMediaCatalogSource {Snapshot value=new Snapshot(19,List.of());public Snapshot nextSnapshot(UUID id){return value;}}
    static class FakeEvents implements DomainEventPublisher {List<ArticleMediaCatalogSnapshotEvent> values=new ArrayList<>();public void publish(DomainEvent event){values.add((ArticleMediaCatalogSnapshotEvent)event);}}
    @Test void empty_inventory_is_a_fact_that_can_retire_previous_usages(){
        var source=new FakeSource();var events=new FakeEvents();var article=UUID.randomUUID();new PublishArticleMediaCatalogSnapshot(source,events,()->at).publish(article);
        assertThat(events.values).hasSize(1);var event=events.values.getFirst();assertThat(event.articleId()).isEqualTo(article);assertThat(event.version()).isEqualTo(19);assertThat(event.references()).isEmpty();assertThat(event.parts()).isEqualTo(1);assertThat(event.occurredAt()).isEqualTo(at);
    }
    @Test void large_inventories_are_bounded_parts_with_one_ordering_version()throws Exception{
        var source=new FakeSource();var events=new FakeEvents();var refs=new ArrayList<Reference>();
        for(int i=0;i<250;i++)refs.add(new Reference(UUID.randomUUID(),"https://images.test/"+"é\"".repeat(3500)+i,UUID.randomUUID(),UUID.randomUUID(),1,"Article","DRAFT","COVER",null,0,"Alt",1200,800,true,false,null,null,null,null,null,null));
        source.value=new ArticleMediaCatalogSource.Snapshot(22,refs);new PublishArticleMediaCatalogSnapshot(source,events,()->at).publish(UUID.randomUUID());
        assertThat(events.values.size()).isGreaterThan(1);assertThat(events.values.stream().flatMap(e->e.references().stream()).toList()).isEqualTo(refs);
        var json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        for(int i=0;i<events.values.size();i++){var e=events.values.get(i);assertThat(e.part()).isEqualTo(i);assertThat(e.parts()).isEqualTo(events.values.size());assertThat(e.version()).isEqualTo(22);assertThat(e.references().size()).isLessThanOrEqualTo(100);assertThat(json.writeValueAsBytes(e).length).isLessThan(200000);}
    }
    static class FakeScan implements ArticleMediaCatalogScan {Batch batch=new Batch(true,List.of(UUID.randomUUID()));int requested;UUID cursor;boolean complete;public Batch lockNext(int limit){requested=limit;return batch;}public void advance(UUID cursor,boolean complete){this.cursor=cursor;this.complete=complete;}}
    @Test void replay_advances_only_after_all_publications_and_observes_the_due_checkpoint(){
        var scan=new FakeScan();var publisher=new FakeArticleMediaCatalogPublisher();var replay=new ReplayArticleMediaCatalog(scan,publisher);
        assertThat(replay.nextBatch()).isEqualTo(1);assertThat(scan.requested).isEqualTo(100);assertThat(scan.complete).isTrue();assertThat(publisher.published).isEqualTo(scan.batch.articleIds());
        scan.batch=new ArticleMediaCatalogScan.Batch(false,List.of());assertThat(replay.nextBatch()).isZero();assertThat(publisher.published).hasSize(1);
    }
    @Test void replay_failure_does_not_advance_the_checkpoint(){
        var scan=new FakeScan();var replay=new ReplayArticleMediaCatalog(scan,id->{throw new IllegalStateException("outbox unavailable");});
        assertThatThrownBy(replay::nextBatch).isInstanceOf(IllegalStateException.class);assertThat(scan.cursor).isNull();assertThat(scan.complete).isFalse();
    }
}
