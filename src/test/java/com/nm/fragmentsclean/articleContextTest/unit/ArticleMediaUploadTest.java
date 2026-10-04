package com.nm.fragmentsclean.articleContextTest.unit;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaUpload;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.RecordArticleMediaUpload;
import java.time.Instant;import java.util.*;import org.junit.jupiter.api.Test;import static org.assertj.core.api.Assertions.*;
class ArticleMediaUploadTest {
 static class FakeUploads implements ArticleMediaUploadRepository {List<ArticleMediaUpload> values=new ArrayList<>();boolean fail;public void save(ArticleMediaUpload upload){if(fail)throw new IllegalStateException("registry unavailable");values.add(upload);}}
 @Test void records_actual_stored_size_measured_dimensions_and_operator_then_publishes(){
  var repo=new FakeUploads();var publisher=new FakeArticleMediaCatalogPublisher();var at=Instant.parse("2026-10-04T12:00:00Z");var article=UUID.randomUUID();var actor=UUID.randomUUID();
  new RecordArticleMediaUpload(repo,bytes->new ArticleImageMetadataReader.Dimensions(320,240),publisher,()->at).record(article,"https://images.test/a.png","a.png","image/png",new byte[]{1,2,3},1200,800,actor,"STUDIO");
  var value=repo.values.getFirst();assertThat(value.width()).isEqualTo(320);assertThat(value.height()).isEqualTo(240);assertThat(value.size()).isEqualTo(3);assertThat(value.uploadedAt()).isEqualTo(at);assertThat(value.uploadedBy()).isEqualTo(actor);assertThat(publisher.published).containsExactly(article);
 }
 @Test void registry_failure_does_not_publish_and_generated_upload_has_no_invented_actor(){
  var repo=new FakeUploads();var publisher=new FakeArticleMediaCatalogPublisher();var usecase=new RecordArticleMediaUpload(repo,bytes->new ArticleImageMetadataReader.Dimensions(null,null),publisher,Instant::now);var article=UUID.randomUUID();
  usecase.record(article,"https://images.test/generated.png",null,"image/png",new byte[]{1},640,480,null,"GENERATION");assertThat(repo.values.getFirst().uploadedBy()).isNull();assertThat(repo.values.getFirst().purpose()).isEqualTo("GENERATION");
  repo.fail=true;assertThatThrownBy(()->usecase.record(article,"https://images.test/failed.png",null,"image/png",new byte[]{1},null,null,null,"GENERATION")).isInstanceOf(IllegalStateException.class);assertThat(publisher.published).hasSize(1);
 }
}
