package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.adapters.primary.springboot.scheduling.ScheduledArticleGenerationRequester;
import com.nm.fragmentsclean.aticleContext.write.adapters.primary.springboot.sqs.ArticleGenerationRequestedSqsIntegrationEventHandler;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleGenerationIdPort;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.RequestArticleGenerationCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.ScheduleArticleGeneration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleGenerationRuntimeWiringTest {

    @Test
    void generation_consumer_is_conditioned_on_the_same_property_as_the_openai_provider() {
        var condition = ArticleGenerationRequestedSqsIntegrationEventHandler.class
                .getAnnotation(ConditionalOnProperty.class);

        assertThat(condition).isNotNull();
        assertThat(condition.name()).containsExactly("fragments.article.generation.openai.enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
    }

    @Test
    void scheduler_requests_initial_work_after_application_ready() {
        var commands = new ArrayDeque<RequestArticleGenerationCommand>();
        var ids = new ArrayDeque<UUID>();
        ids.add(UUID.randomUUID());
        ids.add(UUID.randomUUID());
        ids.add(UUID.randomUUID());
        ids.add(UUID.randomUUID());
        ArticleGenerationIdPort idPort = ids::remove;
        var schedule = new ScheduleArticleGeneration(commands::add, (subject, now, max, dedup) -> true,
                idPort, () -> Instant.parse("2026-08-28T12:00:00Z"));
        var requester = new ScheduledArticleGenerationRequester(schedule, "Culture café", "fr-FR", 2, 168);

        requester.requestOnApplicationReady(null);

        assertThat(commands).singleElement().satisfies(command ->
                assertThat(command.theme()).isEqualTo("Culture café"));
    }

    @Test
    void recurring_tick_waits_one_full_cadence_after_startup() throws NoSuchMethodException {
        var annotation = ScheduledArticleGenerationRequester.class
                .getMethod("requestIfConfigured")
                .getAnnotation(Scheduled.class);

        assertThat(annotation.fixedDelayString())
                .isEqualTo("${fragments.article.generation.schedule.delay-ms:604800000}");
        assertThat(annotation.initialDelayString())
                .isEqualTo("${fragments.article.generation.schedule.delay-ms:604800000}");
    }
}
