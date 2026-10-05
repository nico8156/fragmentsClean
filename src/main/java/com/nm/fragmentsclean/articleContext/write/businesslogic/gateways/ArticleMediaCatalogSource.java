package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;
import java.util.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaCatalogSnapshotEvent.Reference;
public interface ArticleMediaCatalogSource {
    Snapshot nextSnapshot(UUID articleId);
    record Snapshot(long version,List<Reference> references){public Snapshot{references=List.copyOf(references);}}
}
