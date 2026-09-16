package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleAggregate;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class ArticleLocaleTest {
    @Test
    void generated_article_shell_owns_canonical_locale_before_persistence() {
        var article = ArticleAggregate.awaitingGeneration(UUID.randomUUID(), "coffee", "fr",
                UUID.randomUUID(), "Studio", Instant.now());
        assertThat(article.locale()).isEqualTo("fr-FR");
    }
}
