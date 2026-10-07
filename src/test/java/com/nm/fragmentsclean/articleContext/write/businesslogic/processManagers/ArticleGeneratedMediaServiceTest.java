package com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ArticleGeneratedMediaServiceTest {
    @Test void generatesOneCoverAndOneStableImagePerSection(){
        var generator=new FakeGenerator(); var storage=new FakeStorage(); var service=new ArticleGeneratedMediaService(generator,storage, new com.nm.fragmentsclean.articleContextTest.unit.FakeArticleMediaUploadRecorder());
        var draft=GeneratedArticleDraft.from("Titre","Introduction","Conclusion","Couverture",List.of(section("A"),section("B"),section("C")),List.of(ArticleEditorialTag.DECOUVERTE));
        var result=service.generate(UUID.fromString("00000000-0000-0000-0000-000000000001"),UUID.randomUUID(),draft);
        assertNotNull(result.coverImage()); assertEquals(4,generator.calls); assertEquals(4,storage.ids.size());
        assertTrue(result.sections().stream().allMatch(section->section.content().images().size()==1));
    }
    @Test void replay_keeps_provider_identity_but_stores_new_files_without_overwriting_retired_objects(){
        var generator=new FakeGenerator();var storage=new FakeStorage();var recorder=new com.nm.fragmentsclean.articleContextTest.unit.FakeArticleMediaUploadRecorder();
        var service=new ArticleGeneratedMediaService(generator,storage,recorder);var saga=UUID.randomUUID();var article=UUID.randomUUID();
        var draft=GeneratedArticleDraft.from("Titre","Introduction","Conclusion","Couverture",List.of(section("A"),section("B"),section("C")),List.of(ArticleEditorialTag.DECOUVERTE));
        var first=service.generate(saga,article,draft);var originalFiles=new HashSet<>(storage.ids);
        var second=service.generate(saga,article,draft);
        assertNotEquals(first.coverImage().storageReference(),second.coverImage().storageReference());
        assertEquals(8,storage.ids.size());assertTrue(storage.ids.containsAll(originalFiles));
        assertEquals(8,recorder.references.size());assertEquals(8,new HashSet<>(recorder.references).size());
        assertEquals(generator.ids.get(0),generator.ids.get(4));assertEquals(generator.ids.get(1),generator.ids.get(5));
    }
    @Test void sharesTheSelectedDirectionAcrossCoverAndSections() {
        var generator = new FakeGenerator();
        var service = new ArticleGeneratedMediaService(generator, new FakeStorage(), new com.nm.fragmentsclean.articleContextTest.unit.FakeArticleMediaUploadRecorder());
        var draft = GeneratedArticleDraft.from("Titre", "Introduction", "Conclusion", "Couverture", List.of(section("A"), section("B"), section("C")), List.of(ArticleEditorialTag.DECOUVERTE));
        service.generate(UUID.randomUUID(), UUID.randomUUID(), draft, ArticleArtDirection.CONTEMPLATIVE);
        assertEquals(List.of(ArticleArtDirection.CONTEMPLATIVE, ArticleArtDirection.CONTEMPLATIVE, ArticleArtDirection.CONTEMPLATIVE, ArticleArtDirection.CONTEMPLATIVE), generator.directions);
    }
    private static GeneratedArticleSection section(String heading){return GeneratedArticleSection.from(heading,"Un paragraphe suffisamment clair.","Visuel "+heading);}
    private static final class FakeGenerator implements ArticleImageGenerationProvider {final List<ArticleArtDirection> directions=new ArrayList<>();int calls;final List<UUID> ids=new ArrayList<>(); public GeneratedImage generate(Request request){calls++;ids.add(request.imageId());directions.add(request.artDirection());return new GeneratedImage(new byte[]{1},"image/webp",request.role()==Role.COVER?1024:1536,request.role()==Role.COVER?1536:1024,"fake",null);}}
    private static final class FakeStorage implements GeneratedArticleImageStorage {final Set<UUID> ids=new HashSet<>();public StoredImage store(UUID articleId,UUID imageId,String mediaType,byte[] bytes){ids.add(imageId);return new StoredImage("s3://bucket/"+imageId,0,0);}}
}
