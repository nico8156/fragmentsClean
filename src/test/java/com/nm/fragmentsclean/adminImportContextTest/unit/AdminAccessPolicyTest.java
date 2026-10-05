package com.nm.fragmentsclean.adminImportContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

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

	@Test
	void exclusive_owner_overrides_legacy_email_and_granted_access() {
		var properties = new AdminSecurityProperties();
		var owner = UUID.randomUUID();
		var other = UUID.randomUUID();
		properties.setExclusiveOwnerId(owner.toString());
		properties.setBootstrapEmails("legacy@example.test");
		properties.setBootstrapUserIds(other.toString());
		var repository = new FakeRepository();
		repository.allowedId = other;
		var policy = new AdminAccessPolicy(properties, repository);
		assertThat(policy.isAllowed(jwtAuthentication(owner, "owner@example.test"))).isTrue();
		assertThat(policy.isAllowed(jwtAuthentication(other, "legacy@example.test"))).isFalse();
		assertThat(policy.isAllowed(null)).isFalse();
	}

	@Test
	void invalid_owner_configuration_fails_instead_of_falling_back_to_allowlist() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> new AdminSecurityProperties().setExclusiveOwnerId("not-an-id"))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private static JwtAuthenticationToken jwtAuthentication(UUID userId, String email) {
		var jwt = Jwt.withTokenValue("test-token")
				.header("alg", "HS256")
				.subject(userId.toString())
				.claim("email", email)
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60))
				.build();
		return new JwtAuthenticationToken(jwt, List.of());
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
