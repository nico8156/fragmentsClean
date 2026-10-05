package com.nm.fragmentsclean.experienceContext.read;
import java.util.Optional;
import java.util.UUID;
public interface ExperienceMediaLifecycleReadRepository {
    Optional<ExperienceMediaLifecycleView> byId(UUID mediaId);
}
