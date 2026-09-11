package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.ImageUploadRejectedException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/experiences/{experienceId}/media")
public final class WriteExperienceMediaController {
  private final IssueExperienceMediaUploadIntent intents;
  private final ConfirmExperienceMediaUpload confirmations;
  private final CommandBus commands;

  public WriteExperienceMediaController(IssueExperienceMediaUploadIntent intents,
      ConfirmExperienceMediaUpload confirmations, CommandBus commands) {
    this.intents=intents; this.confirmations=confirmations; this.commands=commands;
  }

  @PostMapping("/upload-intents")
  ResponseEntity<ExperienceMediaUploadIntent> intent(@PathVariable UUID experienceId,
      @RequestBody ExperienceMediaUploadIntentRequest body, @AuthenticationPrincipal Jwt jwt) {
    if (body.mediaId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mediaId is required");
    return ResponseEntity.status(201).body(intents.issue(experienceId, body.mediaId(), user(jwt), body.contentType(), body.size()));
  }

  @PostMapping("/{mediaId}/confirm")
  ResponseEntity<Void> confirm(@PathVariable UUID experienceId, @PathVariable UUID mediaId,
      @RequestBody ExperienceMediaConfirmRequest body, @AuthenticationPrincipal Jwt jwt) {
	if (body.commandId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "commandId is required");
    try {
      confirmations.confirm(body.commandId(), experienceId, mediaId, user(jwt), body.at());
    } catch (ImageUploadRejectedException rejected) {
      throw new BusinessCommandRejectedException(rejected.code(), rejected.getMessage());
    }
    return ResponseEntity.accepted().build();
  }

  @DeleteMapping("/{mediaId}")
  ResponseEntity<Void> delete(@PathVariable UUID experienceId, @PathVariable UUID mediaId,
      @RequestBody ExperienceMediaConfirmRequest body, @AuthenticationPrincipal Jwt jwt) {
	if (body.commandId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "commandId is required");
    commands.dispatch(new DeleteExperienceMediaCommand(body.commandId(), experienceId, mediaId, user(jwt), body.at()));
    return ResponseEntity.accepted().build();
  }

  private static UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
