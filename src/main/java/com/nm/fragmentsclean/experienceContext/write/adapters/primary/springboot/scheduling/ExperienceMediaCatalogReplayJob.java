package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.scheduling;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.ReplayExperienceMediaCatalog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="fragments.media-catalog.replay.enabled",havingValue="true")
public final class ExperienceMediaCatalogReplayJob {
    private final ReplayExperienceMediaCatalog replay;
    public ExperienceMediaCatalogReplayJob(ReplayExperienceMediaCatalog replay){this.replay=replay;}
    @Scheduled(fixedDelayString="${fragments.media-catalog.replay.delay-ms:15000}") public void tick(){replay.nextBatch();}
}
