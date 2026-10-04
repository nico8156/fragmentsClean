package com.nm.fragmentsclean.coffeeContext.read;
import java.time.Instant;import java.util.UUID;
public record CoffeeMediaLifecycleView(UUID mediaId,UUID coffeeId,String status,String coffeeStatus,Instant retiredAt,Instant purgeEligibleAt,boolean canRetire,boolean canRestore,boolean canPurge,Instant purgeRequestedAt,Instant purgedAt){}
