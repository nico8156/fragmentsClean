package com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ArticleGeneratedMediaServiceTest {
    @Test void generatesOneCoverAndOneStableImagePerSection(){
        var generator=new FakeGenerator(); var storage=new FakeStorage(); var service=new ArticleGeneratedMediaService(generator,storage);
        var draft=GeneratedArticleDraft.from("Titre","Introduction","Conclusion","Couverture",List.of(section("A"),section("B"),section("C")),List.of(ArticleEditorialTag.DECOUVERTE));
        var result=service.generate(UUID.fromString("00000000-0000-0000-0000-000000000001"),UUID.randomUUID(),draft);
        assertNotNull(result.coverImage()); assertEquals(4,generator.calls); assertEquals(4,storage.ids.size());
        assertTrue(result.sections().stream().allMatch(section->section.content().images().size()==1));
    }
    private static GeneratedArticleSection section(String heading){return GeneratedArticleSection.from(heading,"Un paragraphe suffisamment clair.","Visuel "+heading);}
    private static final class FakeGenerator implements ArticleImageGenerationProvider {int calls; public GeneratedImage generate(Request request){calls++;return new GeneratedImage(new byte[]{1},"image/webp",request.role()==Role.COVER?1024:1536,request.role()==Role.COVER?1536:1024,"fake",null);}}
    private static final class FakeStorage implements GeneratedArticleImageStorage {final Set<UUID> ids=new HashSet<>();public StoredImage store(UUID articleId,UUID imageId,String mediaType,byte[] bytes){ids.add(imageId);return new StoredImage("s3://bucket/"+imageId,0,0);}}
}
