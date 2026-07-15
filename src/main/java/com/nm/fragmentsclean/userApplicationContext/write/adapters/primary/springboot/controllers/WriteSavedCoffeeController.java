package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.SetSavedCoffeeCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/saved-coffees")
public class WriteSavedCoffeeController {
	private final CommandBus commandBus;

	public WriteSavedCoffeeController(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	@PostMapping
	public ResponseEntity<Void> setSavedCoffee(
			@RequestBody SavedCoffeeRequestDto body,
			@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		var command = new SetSavedCoffeeCommand(
				body.commandId(),
				UUID.fromString(body.savedCoffeeId()),
				userId,
				UUID.fromString(body.coffeeId()),
				body.value(),
				Instant.parse(body.at()));

		try {
			commandBus.dispatch(command);
			return ResponseEntity.accepted().build();
		} catch (IllegalStateException exception) {
			return ResponseEntity.badRequest().build();
		}
	}
}
