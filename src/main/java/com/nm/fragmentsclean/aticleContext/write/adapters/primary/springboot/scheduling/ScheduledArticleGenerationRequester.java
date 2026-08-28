package com.nm.fragmentsclean.aticleContext.write.adapters.primary.springboot.scheduling;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.ScheduleArticleGeneration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "fragments.article.generation.schedule.enabled", havingValue = "true")
public final class ScheduledArticleGenerationRequester {
    private final ScheduleArticleGeneration schedule;
    private final String subject;
    private final String locale;
    private final int maxPending;
    private final int deduplicationHours;

    public ScheduledArticleGenerationRequester(
            ScheduleArticleGeneration schedule,
            @Value("${fragments.article.generation.schedule.subject:}") String subject,
            @Value("${fragments.article.generation.schedule.locale:fr-FR}") String locale,
            @Value("${fragments.article.generation.schedule.max-pending:2}") int maxPending,
            @Value("${fragments.article.generation.schedule.deduplication-hours:168}") int deduplicationHours) {
        this.schedule = schedule;
        this.subject = subject;
        this.locale = locale;
        this.maxPending = maxPending;
        this.deduplicationHours = deduplicationHours;
    }

    @Scheduled(
            fixedDelayString = "${fragments.article.generation.schedule.delay-ms:604800000}",
            initialDelayString = "${fragments.article.generation.schedule.delay-ms:604800000}")
    public void requestIfConfigured() {
        schedule.execute(subject, locale, maxPending, deduplicationHours);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void requestOnApplicationReady(ApplicationReadyEvent event) {
        requestIfConfigured();
    }
}
