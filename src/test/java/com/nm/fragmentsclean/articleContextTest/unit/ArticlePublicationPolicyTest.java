package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleDomainException;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticlePublicationPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticlePublicationPolicyTest {
    private final ArticlePublicationPolicy policy = new ArticlePublicationPolicy();

    @Test
    void warns_at_the_threshold_but_allows_publication() {
        assertThat(policy.evaluate(24).warning()).isTrue();
        assertThat(policy.evaluate(29).warning()).isTrue();
    }

    @Test
    void rejects_at_the_hard_limit() {
        assertThatThrownBy(() -> policy.evaluate(30))
                .isInstanceOf(ArticleDomainException.class);
    }

    @Test
    void rejects_invalid_count() {
        assertThatThrownBy(() -> policy.evaluate(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
