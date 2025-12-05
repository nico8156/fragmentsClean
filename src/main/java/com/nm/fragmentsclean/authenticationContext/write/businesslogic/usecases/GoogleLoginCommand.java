package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

public record GoogleLoginCommand(
        String code,
        String codeVerifier,
        String redirectUri
) {
}
