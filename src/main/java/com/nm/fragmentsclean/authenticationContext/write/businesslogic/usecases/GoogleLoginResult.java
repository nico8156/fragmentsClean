package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import java.util.UUID;

public record GoogleLoginResult(
		String accessToken,
		String refreshToken,
		UUID userId, // = authUserId (et futur appUserId)
		String displayName,
		String email,
		String avatarUrl) {
}
