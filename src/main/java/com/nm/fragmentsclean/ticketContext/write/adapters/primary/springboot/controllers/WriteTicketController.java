package com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class WriteTicketController {

    private final CommandBus commandBus;

    public WriteTicketController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    /**
     * Verify/Submit ticket: either ocrText or imageRef required.
     * Returns 202 ACCEPTED (async verification).
     */
    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@RequestBody TicketVerifyRequestDto body,
                                       @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        var command = new VerifyTicketCommand(
                UUID.fromString(body.commandId()),
                UUID.fromString(body.ticketId()),
                userId,
                normalizeBlank(body.imageRef()),
                normalizeBlank(body.ocrText()),
                Instant.parse(body.clientAt())
        );

        try {
            commandBus.dispatch(command);
            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            e.printStackTrace(); // TEMP DEBUG
            return ResponseEntity.badRequest().build();
        }

    }

    private String normalizeBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
