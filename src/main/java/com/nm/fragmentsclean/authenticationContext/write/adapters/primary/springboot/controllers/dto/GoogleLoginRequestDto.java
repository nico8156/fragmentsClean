package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto;

public record GoogleLoginRequestDto(
        String authorizationCode // = serverAuthCode
) {
}
