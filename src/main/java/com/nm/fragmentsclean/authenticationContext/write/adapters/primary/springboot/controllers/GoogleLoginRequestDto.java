package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

public record GoogleLoginRequestDto(
        String authorizationCode // = serverAuthCode
) {
}
