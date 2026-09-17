package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities.RefreshTokenJpaEntity;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenHasher;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

  private final SpringRefreshTokenRepository springRepo;
  private final RefreshTokenHasher refreshTokenHasher;

  public JpaRefreshTokenRepository(
      SpringRefreshTokenRepository springRepo,
      RefreshTokenHasher refreshTokenHasher) {
    this.springRepo = springRepo;
    this.refreshTokenHasher = refreshTokenHasher;
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    return springRepo.findByTokenHash(refreshTokenHasher.hash(token)).map(this::toDomain);
  }

  @Override
  public Optional<RefreshToken> findByTokenForUpdate(String token) {
    return springRepo.findByTokenHashForUpdate(refreshTokenHasher.hash(token)).map(this::toDomain);
  }

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    RefreshTokenJpaEntity entity = toEntity(refreshToken);
    RefreshTokenJpaEntity saved = springRepo.save(entity);
    return toDomain(saved);
  }

  @Override
  public List<RefreshToken> findAllByUserId(UUID userId) {
    return springRepo.findAllByUserId(userId).stream().map(this::toDomain).toList();
  }

  private RefreshToken toDomain(RefreshTokenJpaEntity e) {
    return RefreshToken.rehydrate(
        e.getId(), e.getUserId(), e.getTokenHash(), e.getExpiresAt(), e.isRevoked());
  }

  private RefreshTokenJpaEntity toEntity(RefreshToken rt) {
    return new RefreshTokenJpaEntity(
        rt.id(), rt.userId(), rt.tokenHash(), rt.expiresAt(), rt.revoked());
  }
}
