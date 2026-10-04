package com.nm.fragmentsclean.articleContext.read;
import java.util.List;
public record ArticleMediaUsagePage(List<ArticleMediaUsageView> items,String nextCursor) {
    public ArticleMediaUsagePage {items=List.copyOf(items);}
}
