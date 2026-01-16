package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import java.util.UUID;

public record GoogleLoginResult(
		String accessToken,
		String refreshToken,
		UUID authUserId,
		UUID appUserId,
		String displayName,
		String email,
		String avatarUrl) {
}
