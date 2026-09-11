package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import java.util.UUID;
import org.springframework.http.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;

@RestController
public final class WriteExperienceController{
    private final CommandBus commands;public WriteExperienceController(CommandBus commands){this.commands=commands;}
    @PostMapping("/api/experiences")ResponseEntity<Void> create(@RequestBody CreateExperienceRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new CreateExperienceCommand(body.commandId(),body.experienceId(),user(jwt),body.coffeeId(),body.message(),body.publicationStatus(),body.at()));return ResponseEntity.accepted().build();}
    @PatchMapping("/api/experiences/{id}")ResponseEntity<Void> update(@PathVariable UUID id,@RequestBody UpdateExperienceRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new UpdateExperienceCommand(body.commandId(),id,user(jwt),body.message(),body.at()));return ResponseEntity.accepted().build();}
    @PostMapping("/api/experiences/{id}/publish")ResponseEntity<Void> publish(@PathVariable UUID id,@RequestBody ExperienceCommandRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new PublishExperienceCommand(body.commandId(),id,user(jwt),body.at()));return ResponseEntity.accepted().build();}
    @DeleteMapping("/api/experiences/{id}")ResponseEntity<Void> delete(@PathVariable UUID id,@RequestBody ExperienceCommandRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new DeleteExperienceCommand(body.commandId(),id,user(jwt),body.at()));return ResponseEntity.accepted().build();}
    @PostMapping("/api/experiences/{id}/reports")ResponseEntity<Void> report(@PathVariable UUID id,@RequestBody ReportExperienceRequest body,@AuthenticationPrincipal Jwt jwt){commands.dispatch(new ReportExperienceCommand(body.commandId(),body.reportId(),id,user(jwt),body.reason(),body.details(),body.at()));return ResponseEntity.accepted().build();}
    private static UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
