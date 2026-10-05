package com.nm.fragmentsclean.mediaCatalogContext.read;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleMediaCatalogSnapshotIntegrationEvent;
public final class ArticleMediaCatalogHandler {
    private final ArticleMediaCatalogProjection projection;
    public ArticleMediaCatalogHandler(ArticleMediaCatalogProjection projection){this.projection=projection;}
    public boolean handle(ArticleMediaCatalogSnapshotIntegrationEvent event){return projection.apply(event);}
}
