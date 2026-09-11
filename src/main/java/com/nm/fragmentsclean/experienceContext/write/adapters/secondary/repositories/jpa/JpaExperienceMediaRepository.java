package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities.ExperienceMediaJpaEntity;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

public final class JpaExperienceMediaRepository implements ExperienceMediaRepository {
  private final SpringExperienceMediaRepository repository;
  public JpaExperienceMediaRepository(SpringExperienceMediaRepository repository) { this.repository = repository; }
  @Override public Optional<ExperienceMedia> byId(UUID id) { return repository.findByIdForUpdate(id).map(e -> ExperienceMedia.fromSnapshot(e.snapshot())); }
  @Override public Optional<ExperienceMedia.Snapshot> inspect(UUID id) { return repository.findByMediaId(id).map(ExperienceMediaJpaEntity::snapshot); }
  @Override public void save(ExperienceMedia media) { repository.save(new ExperienceMediaJpaEntity(media.snapshot())); }
  @Override public long activeCount(UUID id) { return repository.countActive(id, List.of(ExperienceMediaStatus.PENDING, ExperienceMediaStatus.AVAILABLE)); }
  @Override public boolean hasPending(UUID id) { return repository.existsByExperienceIdAndStatus(id, ExperienceMediaStatus.PENDING); }
  @Override public List<ExperienceMedia> byExperience(UUID id) { return repository.findByExperienceId(id).stream().map(e -> ExperienceMedia.fromSnapshot(e.snapshot())).toList(); }
  @Override public List<ExperienceMedia> cleanupCandidates(Instant before, int limit) { return repository.findCleanupCandidates(ExperienceMediaStatus.DELETION_PENDING, ExperienceMediaStatus.PENDING, before, PageRequest.of(0, limit)).stream().map(e -> ExperienceMedia.fromSnapshot(e.snapshot())).toList(); }
}
