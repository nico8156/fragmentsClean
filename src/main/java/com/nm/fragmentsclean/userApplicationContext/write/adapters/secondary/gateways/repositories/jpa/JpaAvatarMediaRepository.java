package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.AvatarMediaJpaEntity;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AvatarMediaRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMedia;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMediaStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

public final class JpaAvatarMediaRepository implements AvatarMediaRepository{
  private final SpringAvatarMediaRepository repository;
  public JpaAvatarMediaRepository(SpringAvatarMediaRepository repository){this.repository=repository;}
  @Override public Optional<AvatarMedia> byId(UUID id){return repository.findByIdForUpdate(id).map(e->AvatarMedia.fromSnapshot(e.snapshot()));}
  @Override public Optional<AvatarMedia.Snapshot> inspect(UUID id){return repository.findByMediaId(id).map(AvatarMediaJpaEntity::snapshot);}
  @Override public Optional<AvatarMedia> activeByUser(UUID id){return repository.findActiveByUser(id,AvatarMediaStatus.AVAILABLE).map(e->AvatarMedia.fromSnapshot(e.snapshot()));}
  @Override public void save(AvatarMedia media){repository.save(new AvatarMediaJpaEntity(media.snapshot()));}
  @Override public void replaceActive(AvatarMedia previous,AvatarMedia replacement){repository.saveAndFlush(new AvatarMediaJpaEntity(previous.snapshot()));repository.save(new AvatarMediaJpaEntity(replacement.snapshot()));}
  @Override public List<AvatarMedia> cleanupCandidates(Instant before,int limit){return repository.findCleanupCandidates(AvatarMediaStatus.DELETION_PENDING,AvatarMediaStatus.PENDING,before,PageRequest.of(0,limit)).stream().map(e->AvatarMedia.fromSnapshot(e.snapshot())).toList();}
}
