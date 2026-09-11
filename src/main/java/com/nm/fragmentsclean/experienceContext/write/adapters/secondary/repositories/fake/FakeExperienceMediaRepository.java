package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.fake;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import java.time.Instant;
import java.util.*;

public final class FakeExperienceMediaRepository implements ExperienceMediaRepository {
  private final Map<UUID, ExperienceMedia.Snapshot> rows = new HashMap<>();
  @Override public Optional<ExperienceMedia> byId(UUID id) { return Optional.ofNullable(rows.get(id)).map(ExperienceMedia::fromSnapshot); }
  @Override public Optional<ExperienceMedia.Snapshot> inspect(UUID id) { return Optional.ofNullable(rows.get(id)); }
  @Override public void save(ExperienceMedia media) { rows.put(media.id(), media.snapshot()); }
  @Override public long activeCount(UUID id) { return rows.values().stream().filter(s -> s.experienceId().equals(id) && (s.status()==ExperienceMediaStatus.PENDING || s.status()==ExperienceMediaStatus.AVAILABLE)).count(); }
  @Override public boolean hasPending(UUID id) { return rows.values().stream().anyMatch(s -> s.experienceId().equals(id) && s.status()==ExperienceMediaStatus.PENDING); }
  @Override public List<ExperienceMedia> byExperience(UUID id) { return rows.values().stream().filter(s -> s.experienceId().equals(id)).map(ExperienceMedia::fromSnapshot).toList(); }
  @Override public List<ExperienceMedia> cleanupCandidates(Instant before, int limit) { return rows.values().stream().filter(s -> s.status()==ExperienceMediaStatus.DELETION_PENDING || (s.status()==ExperienceMediaStatus.PENDING && s.updatedAt().isBefore(before))).limit(limit).map(ExperienceMedia::fromSnapshot).toList(); }
}
