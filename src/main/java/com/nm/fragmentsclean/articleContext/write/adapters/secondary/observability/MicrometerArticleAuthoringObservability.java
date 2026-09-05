package com.nm.fragmentsclean.articleContext.write.adapters.secondary.observability;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleAuthoringObservability;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public final class MicrometerArticleAuthoringObservability implements ArticleAuthoringObservability {
    private final MeterRegistry meters;

    public MicrometerArticleAuthoringObservability(MeterRegistry meters) {
        this.meters = meters;
    }

    @Override
    public void generationRequested(ArticleAuthoringTrigger trigger) {
        meters.counter("fragments.article.generation.requested", "trigger", trigger.name().toLowerCase()).increment();
    }

    @Override
    public void leaseClaimed(boolean recovered) {
        meters.counter("fragments.article.generation.lease.claimed", "recovered", Boolean.toString(recovered)).increment();
    }

    @Override
    public void generationCompleted() {
        meters.counter("fragments.article.generation.completed").increment();
    }

    @Override
    public void generationFailed(String category) {
        meters.counter("fragments.article.generation.failed", "category", safeCategory(category)).increment();
    }

    private static String safeCategory(String category) {
        if (category == null || category.isBlank()) return "unknown";
        return category.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
    }
}
