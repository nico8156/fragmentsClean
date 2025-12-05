package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

public record GoogleLoginRequestDto(
        String code,
        String codeVerifier,
        String redirectUri
) {
}
