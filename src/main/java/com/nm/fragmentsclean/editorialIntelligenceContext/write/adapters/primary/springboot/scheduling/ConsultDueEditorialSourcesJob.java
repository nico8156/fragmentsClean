package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.primary.springboot.scheduling;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ScheduleDueEditorialSourceConsultations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "fragments.editorial.discovery.schedule.enabled", havingValue = "true")
public final class ConsultDueEditorialSourcesJob {
    private final ScheduleDueEditorialSourceConsultations schedule;
    public ConsultDueEditorialSourcesJob(ScheduleDueEditorialSourceConsultations schedule) { this.schedule = schedule; }
    @Scheduled(fixedDelayString = "${fragments.editorial.discovery.schedule.delay-ms:900000}")
    public void consultDueSources() { schedule.execute(20, Duration.ofMinutes(5), "editorial-discovery"); }
}
