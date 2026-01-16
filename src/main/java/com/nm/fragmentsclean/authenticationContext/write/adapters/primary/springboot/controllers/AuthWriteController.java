package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.GoogleLoginRequestDto;
import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.GoogleLoginResponseDto;
import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.LogoutRequestDto;
import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.RefreshTokenRequestDto;
import com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers.dto.RefreshTokenResponseDto;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginCommand;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginResult;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.LogoutCommand;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.RefreshTokenCommand;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.RefreshTokenResult;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;

@RestController
@RequestMapping("/auth")
public class AuthWriteController {

	private final CommandBus commandBus;

	public AuthWriteController(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	@PostMapping("/google/exchange")
	public ResponseEntity<GoogleLoginResponseDto> googleExchange(@RequestBody GoogleLoginRequestDto body) {

		GoogleLoginCommand command = new GoogleLoginCommand(
				body.authorizationCode());

		GoogleLoginResult result = commandBus.dispatchWithResult(command);

		var userSummary = new GoogleLoginResponseDto.UserSummary(
				result.authUserId(),
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
}
