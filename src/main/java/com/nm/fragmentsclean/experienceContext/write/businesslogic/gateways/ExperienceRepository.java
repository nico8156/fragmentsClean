package com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.Experience;
import java.util.Optional;
import java.util.UUID;

public interface ExperienceRepository {
    Optional<Experience> byId(UUID experienceId);
    void save(Experience experience);
}
