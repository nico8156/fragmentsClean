package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto;

public record AppleMobileLoginRequestDto(
    String identityToken, String authorizationCode, String displayName) {}
