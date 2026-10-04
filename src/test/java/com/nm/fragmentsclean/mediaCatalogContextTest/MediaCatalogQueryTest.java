package com.nm.fragmentsclean.mediaCatalogContextTest;
import com.nm.fragmentsclean.mediaCatalogContext.read.*;
import java.util.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class MediaCatalogQueryTest {
    @Test void preview_batch_preserves_origin_identity_even_when_two_origins_share_a_uuid() {
        UUID id=UUID.randomUUID();
        var repository=new FakeCatalogueReads(List.of(view("EXPERIENCE",id),view("COFFEE",id)));
        var previews=new FakeCurrentPreviews(id);
        var result=new MediaCatalogQueryHandler(repository,previews).handle(new SearchMediaCatalogQuery("",null,null,null,null,30));
        assertThat(previews.requests).containsExactly(List.of(id));
        assertThat(result.items().getFirst().previewUrl()).isEqualTo("https://preview.test/current");
        assertThat(result.items().get(1).previewUrl()).isNull();
        assertThat(result.items().getFirst().id()).isNotEqualTo(result.items().get(1).id());
    }
    private static MediaCatalogView view(String origin,UUID id){return new MediaCatalogView(origin+":"+id,origin,id,UUID.randomUUID(),UUID.randomUUID(),"AVAILABLE",null,"image/jpeg",1024L,640,480,null,Instant.parse("2026-10-04T10:00:00Z"));}
    private record FakeCatalogueReads(List<MediaCatalogView> items) implements MediaCatalogReadRepository {
        public MediaCatalogPage search(SearchMediaCatalogQuery q){return new MediaCatalogPage(items,null,List.of("EXPERIENCE"));}
        public Optional<MediaCatalogView> byId(String id){return items.stream().filter(v->v.id().equals(id)).findFirst();}
    }
    private static final class FakeCurrentPreviews implements MediaCatalogPreviewPort {
        final UUID id;final List<List<UUID>> requests=new ArrayList<>();
        FakeCurrentPreviews(UUID id){this.id=id;}
        public Map<UUID,String> experiencePreviews(List<UUID> ids){requests.add(ids);return Map.of(id,"https://preview.test/current");}
    }
}
