package com.nm.fragmentsclean.coffeeContext.write.adapters.primary.springboot.scheduling;
import org.springframework.stereotype.Component;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.scheduling.annotation.Scheduled;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CleanCoffeeMediaObjects;
@Component @ConditionalOnProperty(name="fragments.coffee.media-purge.enabled",havingValue="true")
public final class CoffeeMediaCleanupJob {private final CleanCoffeeMediaObjects cleanup;public CoffeeMediaCleanupJob(CleanCoffeeMediaObjects cleanup){this.cleanup=cleanup;}@Scheduled(fixedDelayString="${fragments.coffee.media-purge.delay-ms:600000}")public void clean(){cleanup.run(100);}}
