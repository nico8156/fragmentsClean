package com.nm.fragmentsclean.articleContext.write.businesslogic.eventing;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaCatalogSnapshotEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.util.Optional;
import org.springframework.stereotype.Component;
@Component
public final class ArticleMediaCatalogOutboxMetadata implements OutboxEventMetadataContributor {
    public Optional<OutboxEventMetadata> resolve(DomainEvent event){
        if(event instanceof ArticleMediaCatalogSnapshotEvent e)return Optional.of(new OutboxEventMetadata("ArticleMediaCatalog",e.articleId().toString(),"article-media:"+e.articleId()));
        return Optional.empty();
    }
}
