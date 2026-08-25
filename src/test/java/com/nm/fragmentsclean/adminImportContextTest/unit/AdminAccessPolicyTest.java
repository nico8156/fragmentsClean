package com.nm.fragmentsclean.adminImportContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminAccessPolicy;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminSecurityProperties;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminUserAccess;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;

class AdminAccessPolicyTest {
	@Test
	void allows_bootstrap_user_by_email_without_persisted_row() {
		var properties = new AdminSecurityProperties();
		properties.setBootstrapEmails("owner@example.test");
		var userId = UUID.randomUUID();
		var authentication = jwtAuthentication(userId, "owner@example.test");

		assertThat(new AdminAccessPolicy(properties, new FakeRepository()).isAllowed(authentication)).isTrue();
	}

	@Test
	void allows_persisted_user_and_rejects_unknown_user() {
		var properties = new AdminSecurityProperties();
		var allowedId = UUID.randomUUID();
		var repository = new FakeRepository();
		repository.allowedId = allowedId;

		assertThat(new AdminAccessPolicy(properties, repository).isAllowed(jwtAuthentication(allowedId, "admin@example.test"))).isTrue();
		assertThat(new AdminAccessPolicy(properties, repository).isAllowed(jwtAuthentication(UUID.randomUUID(), "other@example.test"))).isFalse();
	}

	private static UsernamePasswordAuthenticationToken jwtAuthentication(UUID userId, String email) {
		var jwt = Jwt.withTokenValue("test-token")
				.header("alg", "HS256")
				.subject(userId.toString())
				.claim("email", email)
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		return new UsernamePasswordAuthenticationToken(jwt, "token", List.of());
	}

	private static class FakeRepository implements AdminUserAccessRepository {
		private UUID allowedId;

		@Override public List<AdminUserAccess> list() { return List.of(); }
		@Override public Optional<AdminUserAccess> findByUserId(UUID userId) { return Optional.empty(); }
		@Override public Optional<UUID> findAuthUserIdByEmail(String email) { return Optional.empty(); }
		@Override public boolean isAllowed(UUID userId, String email) { return userId.equals(allowedId); }
		@Override public void grant(UUID userId, UUID grantedBy, Instant grantedAt) { }
		@Override public void revoke(UUID userId) { }
		@Override public long count() { return 0; }
	}
}
