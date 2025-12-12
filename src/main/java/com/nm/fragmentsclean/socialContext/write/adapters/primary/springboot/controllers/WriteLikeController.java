package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/social/likes")
public class WriteLikeController {
    private final CommandBus commandBus;

    public WriteLikeController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping
    public ResponseEntity<Void> like(@RequestBody LikeRequestDto body,
                                     @AuthenticationPrincipal Jwt jwt) {

        // 🔹 1. userId = subject du JWT backend (AppUser.id)
        UUID userId = UUID.fromString(jwt.getSubject());

        var command = new MakeLikeCommand(
                body.commandId(),
                UUID.fromString(body.likeId()),
                userId,                               // 👈 vient du JWT, plus du body
                UUID.fromString(body.targetId()),
                body.value(),
                Instant.parse(body.at())
        );

        try {
            commandBus.dispatch(command);
            // async + ACK en socket -> 202 Accepted
            return ResponseEntity.accepted().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
