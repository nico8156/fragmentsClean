package com.nm.fragmentsclean.articleContextTest.read;
import com.nm.fragmentsclean.articleContext.read.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class ArticleMediaUsageQueryTest {
    private final UUID article=UUID.randomUUID(),revision=UUID.randomUUID();
    private static final class FakeArticleMediaUsageReadRepository implements ArticleMediaUsageReadRepository {
        Optional<Page> page=Optional.empty();ListAdminArticleMediaQuery received;
        public Optional<Page> list(ListAdminArticleMediaQuery query){received=query;return page;}
    }
    private ArticleMediaUsageReadRepository.Usage usage(String reference){return new ArticleMediaUsageReadRepository.Usage(article,revision,1,"Article","ARCHIVED","COVER",null,0,"Couverture",1200,800,false,true,reference);}
    @Test void maps_a_source_usage_without_leaking_the_reference_or_inventing_upload_metadata(){
        var repo=new FakeArticleMediaUsageReadRepository();repo.page=Optional.of(new ArticleMediaUsageReadRepository.Page(List.of(usage("s3://internal/articles/image.jpg")),null));
        var query=new ListAdminArticleMediaQuery(article,null,30);
        var page=new ListAdminArticleMediaQueryHandler(repo,ref->"https://download.test/signed").handle(query).orElseThrow();
        assertThat(repo.received).isEqualTo(query);
        var item=page.items().getFirst();assertThat(item.mediaId()).isEqualTo(ArticleMediaReferenceIdentity.of("s3://internal/articles/image.jpg"));
        assertThat(item.previewUrl()).isEqualTo("https://download.test/signed");assertThat(item.revisionStatus()).isEqualTo("ARCHIVED");assertThat(item.published()).isTrue();assertThat(item.working()).isFalse();
    }
    @Test void missing_article_is_not_an_empty_page(){
        assertThat(new ListAdminArticleMediaQueryHandler(new FakeArticleMediaUsageReadRepository(),ref->ref).handle(new ListAdminArticleMediaQuery(article,null,30))).isEmpty();
    }
    @Test void refusing_a_private_preview_preserves_the_usage(){
        var repo=new FakeArticleMediaUsageReadRepository();repo.page=Optional.of(new ArticleMediaUsageReadRepository.Page(List.of(usage("s3://private/foreign/image.jpg")),null));
        for(RuntimeException failure:List.of(new IllegalArgumentException("Rejected"),new IllegalStateException("Missing configuration"))){
            var page=new ListAdminArticleMediaQueryHandler(repo,ref->{throw failure;}).handle(new ListAdminArticleMediaQuery(article,null,30)).orElseThrow();
            assertThat(page.items()).hasSize(1);assertThat(page.items().getFirst().previewUrl()).isNull();
        }
    }
    @Test void references_are_stable_across_revisions_but_distinct_files_are_not_collapsed(){
        assertThat(ArticleMediaReferenceIdentity.of("  https://images.test/a.jpg  ")).isEqualTo(ArticleMediaReferenceIdentity.of("https://images.test/a.jpg"));
        assertThat(ArticleMediaReferenceIdentity.of("https://images.test/a.jpg")).isNotEqualTo(ArticleMediaReferenceIdentity.of("https://images.test/b.jpg"));
        assertThatThrownBy(()->ArticleMediaReferenceIdentity.of(" ")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void unsafe_preview_schemes_and_credential_urls_are_not_renderable(){
        var repo=new FakeArticleMediaUsageReadRepository();repo.page=Optional.of(new ArticleMediaUsageReadRepository.Page(List.of(usage("https://images.test/a.jpg")),null));
        for(String url:List.of("javascript:alert(1)","data:image/png,hello","//foreign.test/a.jpg","https://user:secret@images.test/a.jpg","/api/articles/image-assets/../secret")){
            var item=new ListAdminArticleMediaQueryHandler(repo,ref->url).handle(new ListAdminArticleMediaQuery(article,null,30)).orElseThrow().items().getFirst();assertThat(item.previewUrl()).isNull();
        }
    }
    @Test void query_rejects_noncanonical_and_out_of_range_cursors_without_echoing_private_input(){
        for(String cursor:List.of("private-reference",revision+":0:-1:0:"+revision+":extra",revision+":9:-1:0:"+revision,revision+":0:-1:-2:"+revision,revision+":01:-1:0:"+revision))
            assertThatThrownBy(()->new ListAdminArticleMediaQuery(article,cursor,30)).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid article media cursor");
        assertThatThrownBy(()->new ListAdminArticleMediaQuery(article,null,101)).isInstanceOf(IllegalArgumentException.class);
    }
}
