package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.GoogleLoginRequestDto;
import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.GoogleLoginResponseDto;
import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.GoogleMobileLoginRequestDto;
import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.LogoutRequestDto;
import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.RefreshTokenRequestDto;
import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.RefreshTokenResponseDto;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginCommand;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginResult;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.LogoutCommand;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.RefreshTokenCommand;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.RefreshTokenResult;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthWriteController {

	private final CommandBus commandBus;
	private final String mobileIosRedirectUri;

	public AuthWriteController(
			CommandBus commandBus,
			@Value("${google.oauth.mobile-ios-redirect-uri:}") String mobileIosRedirectUri) {
		this.commandBus = commandBus;
		this.mobileIosRedirectUri = mobileIosRedirectUri;
	}

	@PostMapping("/google/exchange")
	public ResponseEntity<GoogleLoginResponseDto> googleExchange(@RequestBody GoogleLoginRequestDto body) {

		GoogleLoginCommand command = new GoogleLoginCommand(
				body.authorizationCode());

		GoogleLoginResult result = commandBus.dispatchWithResult(command);

		var userSummary = new GoogleLoginResponseDto.UserSummary(
				result.userId(),
				result.displayName(),
				result.email(),
				result.avatarUrl());

		var response = new GoogleLoginResponseDto(
				result.accessToken(),
				result.refreshToken(),
				userSummary);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/google/mobile")
	public ResponseEntity<GoogleLoginResponseDto> googleMobile(@RequestBody GoogleMobileLoginRequestDto body) {
		requirePresent(body.authorizationCode(), "authorizationCode");
		requirePresent(body.codeVerifier(), "codeVerifier");
		requirePresent(body.redirectUri(), "redirectUri");
		requirePresent(mobileIosRedirectUri, "google.oauth.mobile-ios-redirect-uri");
		if (!mobileIosRedirectUri.equals(body.redirectUri().trim())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirectUri is not allowed");
		}

		GoogleLoginCommand command = GoogleLoginCommand.mobilePkce(
				body.authorizationCode().trim(),
				body.codeVerifier().trim(),
				mobileIosRedirectUri);

		GoogleLoginResult result = commandBus.dispatchWithResult(command);

		var userSummary = new GoogleLoginResponseDto.UserSummary(
				result.userId(),
				result.displayName(),
				result.email(),
				result.avatarUrl());

		var response = new GoogleLoginResponseDto(
				result.accessToken(),
				result.refreshToken(),
				userSummary);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/refresh")
	public ResponseEntity<RefreshTokenResponseDto> refresh(@RequestBody RefreshTokenRequestDto body) {

		var command = new RefreshTokenCommand(body.refreshToken());
		RefreshTokenResult result = commandBus.dispatchWithResult(command);

		var response = new RefreshTokenResponseDto(
				result.accessToken(),
				result.refreshToken());

		return ResponseEntity.ok(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestBody LogoutRequestDto body) {
		var command = new LogoutCommand(body.refreshToken());
		commandBus.dispatch(command);
		return ResponseEntity.noContent().build();
	}

	private static void requirePresent(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
		}
	}
}
