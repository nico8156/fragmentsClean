package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.scheduling;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.ReplayAvatarMediaCatalog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="fragments.media-catalog.replay.enabled",havingValue="true")
public final class AvatarMediaCatalogReplayJob {
    private final ReplayAvatarMediaCatalog replay;
    public AvatarMediaCatalogReplayJob(ReplayAvatarMediaCatalog replay){this.replay=replay;}
    @Scheduled(fixedDelayString="${fragments.media-catalog.replay.delay-ms:15000}") public void tick(){replay.nextBatch();}
}
