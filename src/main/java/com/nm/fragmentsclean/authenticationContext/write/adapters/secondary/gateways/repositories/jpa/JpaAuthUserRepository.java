package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities.AuthUserJpaEntity;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUser;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAuthUserRepository implements AuthUserRepository {

    private final SpringAuthUserRepository springRepo;

    public JpaAuthUserRepository(SpringAuthUserRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<AuthUser> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId) {
        return springRepo.findByProviderAndProviderUserId(provider, providerUserId)
                .map(this::toDomain);
    }

    @Override
    public AuthUser save(AuthUser user) {
        AuthUserJpaEntity entity = toEntity(user);
        AuthUserJpaEntity saved = springRepo.save(entity);
        // on pourrait rehydrater complètement, mais ici c’est déjà cohérent
        return toDomain(saved);
    }

    @Override
    public Optional<AuthUser> findById(UUID id) {
        return springRepo.findById(id)
                .map(this::toDomain);
    }

    private AuthUser toDomain(AuthUserJpaEntity e) {
        // rehydrate aggregate sans events
        return new AuthUser(
                e.getId(),
                e.getProvider(),
                e.getProviderUserId(),
                e.getEmail(),
                e.isEmailVerified(),
                e.getLastLoginAt()
        );
    }

    private AuthUserJpaEntity toEntity(AuthUser user) {
        return new AuthUserJpaEntity(
                user.id(),
                user.provider(),
                user.providerUserId(),
                user.email(),
                user.emailVerified(),
                user.lastLoginAt()
        );
    }
}
