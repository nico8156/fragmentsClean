package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.AppUserJpaEntity;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAppUserRepository implements AppUserRepository {

    private final SpringAppUserRepository springRepo;

    public JpaAppUserRepository(SpringAppUserRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<AppUser> findByAuthUserId(UUID authUserId) {
        return springRepo.findByAuthUserId(authUserId)
                .map(this::toDomain);
    }

    @Override
    public Optional<AppUser> findById(UUID userId) {
        return springRepo.findById(userId)
                .map(this::toDomain);
    }

    @Override
    public AppUser save(AppUser user) {
        AppUserJpaEntity entity = toEntity(user);
        AppUserJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    private AppUser toDomain(AppUserJpaEntity e) {
        return new AppUser(
                e.getId(),
                e.getAuthUserId(),
                e.getDisplayName(),
                e.getAvatarUrl(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVersion()
        );
    }

    private AppUserJpaEntity toEntity(AppUser user) {
        return new AppUserJpaEntity(
                user.id(),
                user.authUserId(),
                user.displayName(),
                user.avatarUrl(),
                user.createdAt(),
                user.updatedAt(),
                user.version()
        );
    }
}
