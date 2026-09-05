package com.nm.fragmentsclean.articleContext.read;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleStudioDraftReader {
    List<ArticleStudioDraftView> list();
    Optional<ArticleStudioDraftView> byId(UUID articleId);
}
