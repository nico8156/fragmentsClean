package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;
import java.util.*;
public interface ArticleMediaCatalogScan {
    Batch lockNext(int limit);void advance(UUID cursor,boolean complete);
    record Batch(boolean due,List<UUID> articleIds){public Batch{articleIds=List.copyOf(articleIds);}}
}
