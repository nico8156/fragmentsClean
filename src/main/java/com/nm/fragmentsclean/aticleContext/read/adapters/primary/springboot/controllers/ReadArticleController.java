package com.nm.fragmentsclean.aticleContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.aticleContext.read.GetArticleBySlugQuery;
import com.nm.fragmentsclean.aticleContext.read.ListArticlesQuery;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleListView;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleView;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
public class ReadArticleController {

	private final QueryBus querryBus;

	public ReadArticleController(QueryBus querryBus) {
		this.querryBus = querryBus;
	}

	@GetMapping("/{slug}")
	public ResponseEntity<ArticleView> getBySlug(
			@PathVariable String slug,
			@RequestParam String locale) {
		var query = new GetArticleBySlugQuery(slug, locale);
		ArticleView view = querryBus.dispatch(query);

		if (view == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(view);
	}

	@GetMapping
	public ResponseEntity<ArticleListView> list(
			@RequestParam String locale,
			@RequestParam(required = false) Integer limit,
			@RequestParam(required = false) String cursor) {
		var query = new ListArticlesQuery(locale, limit, cursor);
		ArticleListView listView = querryBus.dispatch(query);

		return ResponseEntity.ok(listView);
	}
}
