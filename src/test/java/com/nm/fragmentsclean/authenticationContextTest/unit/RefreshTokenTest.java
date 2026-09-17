package com.nm.fragmentsclean.authenticationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;

class RefreshTokenTest {
	private static final Instant EXPIRY = Instant.parse("2026-09-17T12:00:00Z");

	@Test
	void expires_at_the_exact_boundary() {
		var token = RefreshToken.createNew(UUID.randomUUID(), "a".repeat(64), EXPIRY);

		assertThat(token.isExpiredAt(EXPIRY.minusNanos(1))).isFalse();
		assertThat(token.isExpiredAt(EXPIRY)).isTrue();
		assertThat(token.isExpiredAt(EXPIRY.plusNanos(1))).isTrue();
	}

	@Test
	void rejects_a_raw_bearer_instead_of_a_sha256_hash() {
		assertThatThrownBy(() -> RefreshToken.createNew(UUID.randomUUID(), "rft-secret", EXPIRY))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rotation_can_preserve_an_existing_session_family() {
		var familyId = UUID.randomUUID();
		var token = RefreshToken.createInFamily(
				UUID.randomUUID(), "b".repeat(64), EXPIRY, familyId);

		assertThat(token.familyId()).isEqualTo(familyId);
	}
}
