package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.CreateCommentCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.DeleteCommentCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.UpdateCommentCommand;
import org.springframework.http.ResponseEntity;
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

    // --- CREATE -------------------------------------------------------------

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CommentCreateRequestDto body) {

        var command = new CreateCommentCommand(
                UUID.fromString(body.commandId()),
                UUID.fromString(body.commentId()),
                UUID.fromString(body.userId()),
                UUID.fromString(body.targetId()),
                body.parentId() != null && !body.parentId().isBlank()
                        ? UUID.fromString(body.parentId())
                        : null,
                body.body(),
                Instant.parse(body.at())
        );

        try {
            commandBus.dispatch(command);
            return ResponseEntity.accepted().build();
        } catch (IllegalStateException e) {
            // ex: incohérence id, règles métier violées, etc.
            return ResponseEntity.badRequest().build();
        }
    }

    // --- UPDATE ------------------------------------------------------------

    @PutMapping
    public ResponseEntity<Void> update(@RequestBody CommentUpdateRequestDto body) {

        var command = new UpdateCommentCommand(
                UUID.fromString(body.commandId()),
                UUID.fromString(body.commentId()),
                body.body(),
                Instant.parse(body.editedAt())
        );

        try {
            commandBus.dispatch(command);
            return ResponseEntity.accepted().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // --- DELETE (soft) -----------------------------------------------------

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody CommentDeleteRequestDto body) {

        var command = new DeleteCommentCommand(
                UUID.fromString(body.commandId()),
                UUID.fromString(body.commentId()),
                Instant.parse(body.deletedAt())
        );

        try {
            commandBus.dispatch(command);
            return ResponseEntity.accepted().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
