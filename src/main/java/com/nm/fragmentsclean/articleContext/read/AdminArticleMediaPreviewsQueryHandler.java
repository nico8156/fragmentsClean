package com.nm.fragmentsclean.articleContext.read;
import java.util.*;
import org.springframework.stereotype.Service;
@Service
public final class AdminArticleMediaPreviewsQueryHandler {
    private final ArticleMediaPreviewReadRepository repository;private final ArticleImageUriResolver previews;
    public AdminArticleMediaPreviewsQueryHandler(ArticleMediaPreviewReadRepository repository,ArticleImageUriResolver previews){this.repository=repository;this.previews=previews;}
    public Map<UUID,String> handle(AdminArticleMediaPreviewsQuery query){
        if(query.references().isEmpty())return Map.of();
        var current=repository.currentReferences(query.references().values());var result=new HashMap<UUID,String>();
        query.references().forEach((id,ref)->{if(current.contains(ref) && ArticleMediaReferenceIdentity.of(ref).equals(id)){var url=ArticleMediaPreviewUrls.resolve(previews,ref);if(url!=null)result.put(id,url);}});
        return Map.copyOf(result);
    }
}
