package com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.controllers;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommand;

@RestController
@RequestMapping("/api/tickets")
public class WriteTicketController {

	private final CommandBus commandBus;

	@Value("${demo.enabled:false}")
	private boolean demoEnabled;

	@Value("${demo.userId:}")
	private String demoUserId;

	public WriteTicketController(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	/**
	 * Verify/Submit ticket: either ocrText or imageRef required.
	 * Returns 202 ACCEPTED (async verification).
	 */
	@PostMapping("/verify")
	public ResponseEntity<Void> verify(@RequestBody TicketVerifyRequestDto body,
			@AuthenticationPrincipal Jwt jwt,
			@RequestHeader(value = "X-DEMO-USER", required = false) String demoUser) {

		UUID userId = resolveUserId(jwt, demoUser);
		if (userId == null) {
			return ResponseEntity.status(401).build();
		}

		var command = new VerifyTicketCommand(
				UUID.fromString(body.commandId()),
				UUID.fromString(body.ticketId()),
				userId,
				normalizeBlank(body.imageRef()),
				normalizeBlank(body.ocrText()),
				Instant.parse(body.clientAt()));

		try {
			commandBus.dispatch(command);
			return ResponseEntity.accepted().build();
		} catch (Exception e) {
			e.printStackTrace(); // TEMP DEBUG
			return ResponseEntity.badRequest().build();
		}
	}

	private UUID resolveUserId(Jwt jwt, String demoUser) {
		if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
			return UUID.fromString(jwt.getSubject());
		}
		if (!demoEnabled) {
			return null;
		}
		String raw = (demoUser != null && !demoUser.isBlank()) ? demoUser : demoUserId;
		if (raw == null || raw.isBlank()) {
			return null;
		}
		return UUID.fromString(raw);
	}

	private String normalizeBlank(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}
}
