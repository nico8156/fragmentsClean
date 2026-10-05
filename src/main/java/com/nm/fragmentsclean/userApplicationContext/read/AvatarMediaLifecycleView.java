package com.nm.fragmentsclean.userApplicationContext.read;
import java.util.UUID;
public record AvatarMediaLifecycleView(UUID mediaId, UUID ownerId, String status, long usages,
    boolean canRetire, boolean canRestore, String retention, boolean canPurge, java.time.Instant retiredAt, java.time.Instant purgeEligibleAt) {}
