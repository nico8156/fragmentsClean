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
	public Optional<AuthUser> findById(UUID id) {
		return springRepo.findById(id).map(this::toDomain);
	}

	@Override
	public AuthUser save(AuthUser user) {
		AuthUserJpaEntity saved = springRepo.save(toEntity(user));
		return toDomain(saved);
	}

	private AuthUser toDomain(AuthUserJpaEntity e) {
		return new AuthUser(
				e.getId(),
				e.getProvider(),
				e.getProviderUserId(),
				e.getEmail(),
				e.isEmailVerified(),
				e.getDisplayName(),
				e.getAvatarUrl(),
				e.getLastLoginAt());
	}

	private AuthUserJpaEntity toEntity(AuthUser u) {
		return new AuthUserJpaEntity(
				u.id(),
				u.provider(),
				u.providerUserId(),
				u.email(),
				u.emailVerified(),
				u.displayName(),
				u.avatarUrl(),
				u.lastLoginAt());
	}
}
