package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto;

import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;

public record GoogleLoginRequestDto(
		@NotNull @JsonAlias("code") // accepte aussi { "code": "..." }
		String authorizationCode) {
}
