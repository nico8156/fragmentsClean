package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.articleContext.write.adapters.secondary.observability.MicrometerArticleAuthoringObservability;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerArticleAuthoringObservabilityTest {
    @Test
    void records_bounded_operational_dimensions() {
        var registry = new SimpleMeterRegistry();
        var metrics = new MicrometerArticleAuthoringObservability(registry);

        metrics.generationRequested(ArticleAuthoringTrigger.SCHEDULED);
        metrics.leaseClaimed(true);
        metrics.generationCompleted();
        metrics.generationFailed("Provider Timeout");

        assertThat(registry.get("fragments.article.generation.requested").tag("trigger", "scheduled").counter().count()).isEqualTo(1);
        assertThat(registry.get("fragments.article.generation.lease.claimed").tag("recovered", "true").counter().count()).isEqualTo(1);
        assertThat(registry.get("fragments.article.generation.completed").counter().count()).isEqualTo(1);
        assertThat(registry.get("fragments.article.generation.failed").tag("category", "provider_timeout").counter().count()).isEqualTo(1);
    }
}
