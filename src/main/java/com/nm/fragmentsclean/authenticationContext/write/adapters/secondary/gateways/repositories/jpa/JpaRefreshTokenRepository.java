package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities.RefreshTokenJpaEntity;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final SpringRefreshTokenRepository springRepo;

    public JpaRefreshTokenRepository(SpringRefreshTokenRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return springRepo.findByToken(token)
                .map(this::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = toEntity(refreshToken);
        RefreshTokenJpaEntity saved = springRepo.save(entity);
        return toDomain(saved);
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.rehydrate(
                e.getId(),
                e.getUserId(),
                e.getToken(),
                e.getExpiresAt(),
                e.isRevoked()
        );
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken rt) {
        return new RefreshTokenJpaEntity(
                rt.id(),
                rt.userId(),
                rt.token(),
                rt.expiresAt(),
                rt.revoked()
        );
    }
}
