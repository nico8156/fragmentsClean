package com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.entities.IdentityJpaEntity;
import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.Identity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaIdentityRepository implements IdentityRepository {

    private final SpringIdentityRepository springRepo;

    public JpaIdentityRepository(SpringIdentityRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<Identity> findByProviderAndProviderUserId(String provider, String providerUserId) {
        return springRepo.findByProviderAndProviderUserId(provider, providerUserId)
                .map(JpaIdentityRepository::toDomain);
    }

    @Override
    public List<Identity> listByUserId(UUID userId) {
        return springRepo.findByUserId(userId).stream()
                .map(JpaIdentityRepository::toDomain)
                .toList();
    }

    @Override
    public Identity save(Identity identity) {
        IdentityJpaEntity entity = toEntity(identity);
        IdentityJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    // ---------- mapping ----------

    private static IdentityJpaEntity toEntity(Identity identity) {
        return new IdentityJpaEntity(
                identity.id(),
                identity.userId(),
                identity.provider(),
                identity.providerUserId(),
                identity.email(),
                identity.createdAt(),
                identity.lastAuthAt()
        );
    }

    private static Identity toDomain(IdentityJpaEntity entity) {
        return new Identity(
                entity.getId(),
                entity.getUserId(),
                entity.getProvider(),
                entity.getProviderUserId(),
                entity.getEmail(),
                entity.getCreatedAt(),
                entity.getLastAuthAt()
        );
    }
}
