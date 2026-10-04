package com.nm.fragmentsclean.coffeeContext.write.adapters.primary.springboot.scheduling;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ReplayCoffeeMediaCatalog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="fragments.media-catalog.replay.enabled",havingValue="true")
public final class CoffeeMediaCatalogReplayJob {
    private final ReplayCoffeeMediaCatalog replay;
    public CoffeeMediaCatalogReplayJob(ReplayCoffeeMediaCatalog replay){this.replay=replay;}
    @Scheduled(fixedDelayString="${fragments.media-catalog.replay.delay-ms:15000}") public void tick(){replay.nextBatch();}
}
