package com.nm.fragmentsclean.articleContextTest.unit;
import java.util.*;import java.time.Instant;
import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaLifecycle;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.*;
class ArticleMediaPurgeTest {
 static class FakeMedia implements ArticleMediaLifecycleRepository {
  ArticleMediaLifecycle item;int changes;FakeMedia(String status,long usages){item=new ArticleMediaLifecycle(UUID.randomUUID(),UUID.randomUUID(),"managed",status,usages,Instant.EPOCH);}
  public Optional<ArticleMediaLifecycle> lock(UUID id){return item.mediaId().equals(id)?Optional.of(item):Optional.empty();}
  public List<UUID> pendingPurgeIds(int limit){return "DELETION_PENDING".equals(item.status())?List.of(item.mediaId()):List.of();}
  public void change(UUID id,String status,Instant at){changes++;item=new ArticleMediaLifecycle(id,item.articleId(),item.reference(),status,item.usages(),at);}
 }
 static class FakeObjects implements ArticleMediaObjectDeletion {boolean fail;List<String> deleted=new ArrayList<>();public void delete(UUID article,String reference){if(fail)throw new IllegalStateException("storage unavailable");deleted.add(reference);}}
 @Test void storage_failure_keeps_pending_then_retry_completes_and_publishes_once(){
  var repo=new FakeMedia("DELETION_PENDING",0);var objects=new FakeObjects();objects.fail=true;var publisher=new FakeArticleMediaCatalogPublisher();var now=Instant.parse("2026-10-04T12:00:00Z");var completion=new ArticleMediaPurgeCompletion(repo,publisher,()->now);var cleaner=new CleanArticleMediaObjects(repo,objects,completion);
  assertThatThrownBy(()->cleaner.run(100)).hasMessageContaining("storage unavailable");assertThat(repo.item.status()).isEqualTo("DELETION_PENDING");assertThat(repo.changes).isZero();assertThat(publisher.published).isEmpty();
  objects.fail=false;cleaner.run(100);cleaner.run(100);completion.complete(repo.item.mediaId());
  assertThat(repo.item.status()).isEqualTo("DELETED");assertThat(repo.item.updatedAt()).isEqualTo(now);assertThat(objects.deleted).containsExactly("managed");assertThat(repo.changes).isOne();assertThat(publisher.published).containsExactly(repo.item.articleId());
 }
 @Test void age_alone_and_referenced_pending_media_never_delete_objects(){
  for(var repo:List.of(new FakeMedia("RETIRED",0),new FakeMedia("DELETION_PENDING",1),new FakeMedia("ACTIVE",0))){var objects=new FakeObjects();var publisher=new FakeArticleMediaCatalogPublisher();new CleanArticleMediaObjects(repo,objects,new ArticleMediaPurgeCompletion(repo,publisher,Instant::now)).run(100);assertThat(objects.deleted).isEmpty();assertThat(repo.changes).isZero();assertThat(publisher.published).isEmpty();}
 }
}
