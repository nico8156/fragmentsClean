package com.nm.fragmentsclean.aticleContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.aticleContext.read.GetArticleBySlugQuery;
import com.nm.fragmentsclean.aticleContext.read.ListArticlesQuery;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleListView;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleView;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QuerryBus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
public class ReadArticleController {

    private final QuerryBus querryBus;

    public ReadArticleController(QuerryBus querryBus) {
        this.querryBus = querryBus;
    }

    // GET /api/articles/{slug}?locale=fr-FR
    @GetMapping("/{slug}")
    public ResponseEntity<ArticleView> getBySlug(
            @PathVariable String slug,
            @RequestParam String locale,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        ArticleView view = querryBus.dispatch(new GetArticleBySlugQuery(slug, locale));

        if (view == null) {
            return ResponseEntity.notFound().build();
        }

        String etag = generateEtagForArticle(view);

        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .body(view);
    }

    // GET /api/articles?locale=fr-FR&limit=10&cursor=123
    @GetMapping
    public ResponseEntity<ArticleListView> list(
            @RequestParam String locale,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor
    ) {
        ArticleListView listView = querryBus.dispatch(new ListArticlesQuery(locale, limit, cursor));

        return ResponseEntity.ok()
                .eTag(listView.etag())
                .body(listView);
    }

    private String generateEtagForArticle(ArticleView view) {
        // ETag simple basé sur id + version
        return "\"" + view.id() + "-v" + view.version() + "\"";
    }
}
