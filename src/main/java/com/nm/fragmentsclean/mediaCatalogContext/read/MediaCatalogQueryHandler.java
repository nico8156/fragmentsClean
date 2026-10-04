package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.*;
import org.springframework.stereotype.Service;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
@Service
public final class MediaCatalogQueryHandler implements QueryHandler<SearchMediaCatalogQuery,MediaCatalogPage> {
    private final MediaCatalogReadRepository repository;
    private final MediaCatalogPreviewPort previews;
    public MediaCatalogQueryHandler(MediaCatalogReadRepository repository,MediaCatalogPreviewPort previews){this.repository=repository;this.previews=previews;}
    public MediaCatalogPage handle(SearchMediaCatalogQuery query){
        var page=repository.search(query);
        return new MediaCatalogPage(withCurrentPreviews(page.items()),page.nextCursor(),page.coverage());
    }
    public Optional<MediaCatalogView> byId(String id){
        SearchMediaCatalogQuery.parseId(id);
        return repository.byId(id).map(view->withCurrentPreviews(List.of(view)).getFirst());
    }
    private List<MediaCatalogView> withCurrentPreviews(List<MediaCatalogView> items){
        var ids=items.stream().filter(v->"EXPERIENCE".equals(v.origin()) && "AVAILABLE".equals(v.status())).map(MediaCatalogView::mediaId).toList();
        var current=ids.isEmpty()?Map.<UUID,String>of():previews.experiencePreviews(ids);
        return items.stream().map(v->new MediaCatalogView(v.id(),v.origin(),v.mediaId(),v.resourceId(),v.ownerId(),v.status(),"EXPERIENCE".equals(v.origin())?current.get(v.mediaId()):null,v.contentType(),v.size(),v.width(),v.height(),v.createdAt(),v.updatedAt())).toList();
    }
}
