package com.nm.fragmentsclean.authenticationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway.Sha256RefreshTokenHasher;

class Sha256RefreshTokenHasherTest {
	private final Sha256RefreshTokenHasher hasher = new Sha256RefreshTokenHasher();

	@Test
	void hashes_without_retaining_or_reproducing_the_bearer() {
		String bearer = "rft-private-value";

		String hash = hasher.hash(bearer);

		assertThat(hash).hasSize(64).matches("[0-9a-f]{64}").doesNotContain(bearer);
		assertThat(hasher.hash(bearer)).isEqualTo(hash);
		assertThat(hasher.hash("another-value")).isNotEqualTo(hash);
	}

	@Test
	void rejects_missing_presented_tokens() {
		assertThatThrownBy(() -> hasher.hash(" ")).isInstanceOf(IllegalArgumentException.class);
	}
}
