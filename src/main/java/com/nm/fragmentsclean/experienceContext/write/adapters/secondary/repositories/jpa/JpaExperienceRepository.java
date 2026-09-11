package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities.ExperienceJpaEntity;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.Experience;
import java.util.*;

public final class JpaExperienceRepository implements ExperienceRepository{
    private final SpringExperienceRepository repository;public JpaExperienceRepository(SpringExperienceRepository repository){this.repository=repository;}
    @Override public Optional<Experience> byId(UUID id){return repository.findByIdForUpdate(id).map(this::domain);}
    @Override public void save(Experience experience){repository.save(entity(experience));}
    private Experience domain(ExperienceJpaEntity e){return Experience.fromSnapshot(new Experience.Snapshot(e.getExperienceId(),e.getUserId(),e.getCoffeeId(),e.getMessage(),e.getPublicationStatus(),e.getModerationStatus(),e.getCreatedAt(),e.getUpdatedAt(),e.getDeletedAt(),e.getVersion()));}
    private ExperienceJpaEntity entity(Experience value){var s=value.toSnapshot();return new ExperienceJpaEntity(s.experienceId(),s.userId(),s.coffeeId(),s.message(),s.publicationStatus(),s.moderationStatus(),s.createdAt(),s.updatedAt(),s.deletedAt(),s.version());}
}
