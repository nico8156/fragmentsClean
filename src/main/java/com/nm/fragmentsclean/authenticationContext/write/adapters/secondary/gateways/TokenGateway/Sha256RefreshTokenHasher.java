package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenHasher;

@Component
public class Sha256RefreshTokenHasher implements RefreshTokenHasher {
	@Override
	public String hash(String presentedToken) {
		if (presentedToken == null || presentedToken.isBlank()) {
			throw new IllegalArgumentException("Refresh token is required");
		}
		try {
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(presentedToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException impossible) {
			throw new IllegalStateException("SHA-256 is unavailable", impossible);
		}
	}
}
