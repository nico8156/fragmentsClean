package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.RequestAccountDeletionCommand;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users/me")
public final class WriteAccountController {
  private final CommandBus commandBus;

  public WriteAccountController(CommandBus commandBus) {
    this.commandBus = commandBus;
  }

  @DeleteMapping
  public ResponseEntity<Void> delete(
      @RequestBody RequestAccountDeletionDto body, @AuthenticationPrincipal Jwt jwt) {
    if (body.commandId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "commandId is required");
    }
    commandBus.dispatch(
        new RequestAccountDeletionCommand(body.commandId(), UUID.fromString(jwt.getSubject())));
    return ResponseEntity.accepted().build();
  }
}
