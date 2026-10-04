package com.nm.fragmentsclean.articleContext.read;
import java.util.*;
public interface ArticleMediaUsageReadRepository {
    Optional<Page> list(ListAdminArticleMediaQuery query);
    record Page(List<Usage> items,String nextCursor){public Page{items=List.copyOf(items);}}
    record Usage(UUID articleId,UUID revisionId,int revisionNumber,String title,String revisionStatus,String role,Integer sectionPosition,int imagePosition,String alt,int width,int height,boolean working,boolean published,String storageReference) {}
}
