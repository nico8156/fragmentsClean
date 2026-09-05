package com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleImageRef;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public final class ArticleGeneratedMediaService {
    private final ArticleImageGenerationProvider generator; private final GeneratedArticleImageStorage storage;
    public ArticleGeneratedMediaService(ArticleImageGenerationProvider generator,GeneratedArticleImageStorage storage){this.generator=generator;this.storage=storage;}
    public GeneratedArticleDraft generate(UUID sagaId,UUID articleId,GeneratedArticleDraft draft){
        String consistency="fragments-"+sagaId; var cover=generateOne(sagaId,articleId,"cover",ArticleImageGenerationProvider.Role.COVER,draft.coverVisualBrief(),consistency,"Illustration de couverture");
        var sections=new ArrayList<ArticleImageRef>();
        for(int i=0;i<draft.sections().size();i++){var section=draft.sections().get(i);sections.add(generateOne(sagaId,articleId,"section-"+i,ArticleImageGenerationProvider.Role.SECTION,section.visualBrief(),consistency,section.content().heading()));}
        return draft.withGeneratedImages(cover,sections);
    }
    private ArticleImageRef generateOne(UUID sagaId,UUID articleId,String slot,ArticleImageGenerationProvider.Role role,com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleVisualBrief brief,String consistency,String alt){
        UUID imageId=UUID.nameUUIDFromBytes((sagaId+":"+slot).getBytes(StandardCharsets.UTF_8));
        var image=generator.generate(new ArticleImageGenerationProvider.Request(sagaId,imageId,role,brief,consistency));
        var stored=storage.store(articleId,imageId,image.mediaType(),image.bytes());
        return ArticleImageRef.from(stored.storageReference(),image.width(),image.height(),alt);
    }
}
