package com.nm.fragmentsclean.articleContext.read;
import java.util.*;import org.springframework.stereotype.Service;
@Service public class ReadArticleMediaLifecycleQueryHandler {
 private final ArticleMediaLifecycleReadRepository repository;public ReadArticleMediaLifecycleQueryHandler(ArticleMediaLifecycleReadRepository repository){this.repository=repository;}
 public Optional<ArticleMediaLifecycleView> handle(ReadArticleMediaLifecycleQuery query){return repository.byId(query.mediaId());}
}
