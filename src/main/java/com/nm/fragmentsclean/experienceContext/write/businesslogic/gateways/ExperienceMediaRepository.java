package com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperienceMediaRepository {
  Optional<ExperienceMedia> byId(UUID mediaId);
  Optional<ExperienceMedia.Snapshot> inspect(UUID mediaId);
  void save(ExperienceMedia media);
  long activeCount(UUID experienceId);
  boolean hasPending(UUID experienceId);
  List<ExperienceMedia> byExperience(UUID experienceId);
  List<ExperienceMedia> cleanupCandidates(Instant pendingBefore, int limit);
}
