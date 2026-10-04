package com.nm.fragmentsclean.articleContext.read;
import java.util.Optional;
import java.net.URI;
import org.springframework.stereotype.Service;
@Service
public final class ListAdminArticleMediaQueryHandler implements com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler<ListAdminArticleMediaQuery,Optional<ArticleMediaUsagePage>> {
    private final ArticleMediaUsageReadRepository repository;
    private final ArticleImageUriResolver previews;
    public ListAdminArticleMediaQueryHandler(ArticleMediaUsageReadRepository repository,ArticleImageUriResolver previews){this.repository=repository;this.previews=previews;}
    public Optional<ArticleMediaUsagePage> handle(ListAdminArticleMediaQuery query){
        return repository.list(query).map(page->new ArticleMediaUsagePage(page.items().stream().map(u->new ArticleMediaUsageView(ArticleMediaReferenceIdentity.of(u.storageReference()),u.articleId(),u.revisionId(),u.revisionNumber(),u.title(),u.revisionStatus(),u.role(),u.sectionPosition(),u.imagePosition(),u.alt(),u.width(),u.height(),u.working(),u.published(),preview(u.storageReference()))).toList(),page.nextCursor()));
    }
    private String preview(String reference){
        try {
            var value=previews.resolve(reference);
            if(value==null)return null;
            if(value.matches("/api/articles/image-assets/[A-Za-z0-9_.-]+") && !value.contains(".."))return value;
            var uri=URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost()!=null && uri.getRawUserInfo()==null?value:null;
        }catch(IllegalArgumentException | IllegalStateException e){return null;}
    }
}
