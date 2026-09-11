package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.CreateCommentCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.DeleteCommentCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.UpdateCommentCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/social/comments")
public class WriteCommentController {

    private final CommandBus commandBus;

    public WriteCommentController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CommentCreateRequestDto body,
                                       @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        var command = new CreateCommentCommand(
                UUID.fromString(body.commandId()),
                UUID.fromString(body.commentId()),
                userId,
                UUID.fromString(body.targetId()),
                body.parentId() != null && !body.parentId().isBlank()
                        ? UUID.fromString(body.parentId())
                        : null,
                body.body(),
                Instant.parse(body.at())
        );

        commandBus.dispatch(command);
        return ResponseEntity.accepted().build();
    }

    @PutMapping
    public ResponseEntity<Void> update(@RequestBody CommentUpdateRequestDto body,
                                       @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        var command = new UpdateCommentCommand(
                UUID.fromString(body.commandId()),
                UUID.fromString(body.commentId()),
                userId,
                body.body(),
                Instant.parse(body.editedAt())
        );

        commandBus.dispatch(command);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody CommentDeleteRequestDto body,
                                       @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        var command = new DeleteCommentCommand(
                UUID.fromString(body.commandId()),
                UUID.fromString(body.commentId()),
                userId,
                Instant.parse(body.deletedAt())
        );

        commandBus.dispatch(command);
        return ResponseEntity.accepted().build();
    }
}
