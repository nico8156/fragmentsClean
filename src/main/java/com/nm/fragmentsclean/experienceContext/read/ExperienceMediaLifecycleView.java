package com.nm.fragmentsclean.experienceContext.read;
import java.util.UUID;
public record ExperienceMediaLifecycleView(UUID mediaId, UUID experienceId, UUID coffeeId,
    UUID ownerId, String status, String publicationStatus, String moderationStatus,
    boolean canHidePublication, boolean canRestorePublication) {}
