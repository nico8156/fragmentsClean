package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.ModerateExperienceCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public final class WriteExperienceModerationController {
    private final CommandBus commands;
    public WriteExperienceModerationController(CommandBus commands) { this.commands=commands; }

    @PostMapping("/experience-moderation/reports/{reportId}/decision")
    ResponseEntity<Void> decide(@PathVariable UUID reportId,@RequestParam UUID experienceId,
            @RequestBody ModerateExperienceRequest body,@AuthenticationPrincipal Jwt jwt) {
        commands.dispatch(new ModerateExperienceCommand(body.commandId(),body.actionId(),reportId,
                experienceId,UUID.fromString(jwt.getSubject()),body.decision(),body.reason(),body.at()));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/experiences/{experienceId}/moderation")
    ResponseEntity<Void> moderate(@PathVariable UUID experienceId,@RequestBody ModerateExperienceRequest body,
            @AuthenticationPrincipal Jwt jwt) {
        commands.dispatch(new ModerateExperienceCommand(body.commandId(),body.actionId(),null,
                experienceId,UUID.fromString(jwt.getSubject()),body.decision(),body.reason(),body.at()));
        return ResponseEntity.accepted().build();
    }
}
