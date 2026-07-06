package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto;

public record GoogleMobileLoginRequestDto(
		String authorizationCode,
		String codeVerifier,
		String redirectUri) {
}
