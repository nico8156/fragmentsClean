package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.ModerateCommentCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.ReportCommentCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.SetUserBlockCommand;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public final class WriteModerationController {
    private final CommandBus commandBus;
    public WriteModerationController(CommandBus commandBus) { this.commandBus = commandBus; }

    @PostMapping("/api/social/comments/{commentId}/reports")
    ResponseEntity<Void> report(@PathVariable UUID commentId, @RequestBody CommentReportRequestDto body,
                                @AuthenticationPrincipal Jwt jwt) {
        commandBus.dispatch(new ReportCommentCommand(body.commandId(), body.reportId(), commentId,
                UUID.fromString(jwt.getSubject()), body.reason(), body.details(), body.at()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/social/blocks")
    ResponseEntity<Void> setBlock(@RequestBody UserBlockRequestDto body, @AuthenticationPrincipal Jwt jwt) {
        commandBus.dispatch(new SetUserBlockCommand(body.commandId(), body.blockId(), UUID.fromString(jwt.getSubject()),
                body.blockedUserId(), body.active(), body.at()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/admin/moderation/reports/{reportId}/decision")
    ResponseEntity<Void> decide(@PathVariable UUID reportId, @RequestBody ModerateCommentRequestDto body,
                                @AuthenticationPrincipal Jwt jwt) {
        commandBus.dispatch(new ModerateCommentCommand(body.commandId(), body.actionId(), reportId, body.commentId(),
                UUID.fromString(jwt.getSubject()), ModerationStatus.valueOf(body.decision().name()), body.reason(), body.at()));
        return ResponseEntity.accepted().build();
    }
}
