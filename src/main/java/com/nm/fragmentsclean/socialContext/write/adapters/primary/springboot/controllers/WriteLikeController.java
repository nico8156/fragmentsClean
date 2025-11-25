package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Void> like(@RequestBody LikeRequestDto body) {

        var command = new MakeLikeCommand(
                UUID.fromString(body.commandId()),
                UUID.fromString(body.likeId()),
                UUID.fromString(body.userId()),
                UUID.fromString(body.targetId()),
                body.value(),
                Instant.parse(body.at())
        );

        try {
            commandBus.dispatch(command);
            // async + ACK en socket -> 202 Accepted ou 204 No Content
            return ResponseEntity.accepted().build();
        } catch (IllegalStateException e) {
            // incohérence métier (likeId ≠ user/target, etc.)
            return ResponseEntity.badRequest().build();
        }
    }
}
