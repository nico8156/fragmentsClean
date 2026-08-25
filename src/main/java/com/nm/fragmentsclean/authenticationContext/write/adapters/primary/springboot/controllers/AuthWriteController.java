package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.google.GoogleOAuthProperties;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthWriteController {

	private final CommandBus commandBus;
	private final String mobileIosRedirectUri;
	private final GoogleOAuthProperties googleOAuthProperties;

	public AuthWriteController(
			CommandBus commandBus,
			@Value("${google.oauth.mobile-ios-redirect-uri:}") String mobileIosRedirectUri,
			GoogleOAuthProperties googleOAuthProperties) {
		this.commandBus = commandBus;
		this.mobileIosRedirectUri = mobileIosRedirectUri;
		this.googleOAuthProperties = googleOAuthProperties;
	}

	@GetMapping("/google/studio/config")
	public StudioGoogleAuthConfig studioConfig() {
		requirePresent(googleOAuthProperties.getStudioClientId(), "google.oauth.studio-client-id");
		requirePresent(googleOAuthProperties.getStudioRedirectUri(), "google.oauth.studio-redirect-uri");
		return new StudioGoogleAuthConfig(
				googleOAuthProperties.getStudioClientId(),
				googleOAuthProperties.getStudioRedirectUri(),
				"https://accounts.google.com/o/oauth2/v2/auth",
				"openid email profile");
	}

	@PostMapping("/google/studio")
	public ResponseEntity<GoogleLoginResponseDto> googleStudio(@RequestBody GoogleMobileLoginRequestDto body) {
		requirePresent(body.authorizationCode(), "authorizationCode");
		requirePresent(body.codeVerifier(), "codeVerifier");
		requirePresent(body.redirectUri(), "redirectUri");
		requirePresent(googleOAuthProperties.getStudioRedirectUri(), "google.oauth.studio-redirect-uri");
		if (!googleOAuthProperties.getStudioRedirectUri().equals(body.redirectUri().trim())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "redirectUri is not allowed");
		}

		GoogleLoginResult google = commandBus.dispatchWithResult(new GoogleLoginCommand(
				body.authorizationCode().trim(), body.codeVerifier().trim(), body.redirectUri().trim(),
				GoogleLoginCommand.Client.STUDIO));
		return ResponseEntity.ok(toResponse(google));
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

		GoogleLoginCommand command = new GoogleLoginCommand(
				body.authorizationCode().trim(),
				body.codeVerifier().trim(),
				mobileIosRedirectUri);

		GoogleLoginResult result = commandBus.dispatchWithResult(command);
		return ResponseEntity.ok(toResponse(result));
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

	private static GoogleLoginResponseDto toResponse(GoogleLoginResult result) {
		var userSummary = new GoogleLoginResponseDto.UserSummary(
				result.userId(),
				result.displayName(),
				result.email(),
				result.avatarUrl());

		return new GoogleLoginResponseDto(
				result.accessToken(),
				result.refreshToken(),
				userSummary);
	}

	public record StudioGoogleAuthConfig(String clientId, String redirectUri, String authorizationUri, String scope) { }
}
