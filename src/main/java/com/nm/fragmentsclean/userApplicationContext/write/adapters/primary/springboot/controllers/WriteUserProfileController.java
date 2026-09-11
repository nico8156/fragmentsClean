package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.UpdateAppUserProfileCommand;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users/me/profile")
public final class WriteUserProfileController {
  private final CommandBus commandBus;

  public WriteUserProfileController(CommandBus commandBus) {
    this.commandBus = commandBus;
  }

  @PatchMapping
  public ResponseEntity<Void> update(
      @RequestBody UpdateProfileRequestDto body, @AuthenticationPrincipal Jwt jwt) {
    if (body.commandId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "commandId is required");
    }
    commandBus.dispatch(
        new UpdateAppUserProfileCommand(
            body.commandId(), UUID.fromString(jwt.getSubject()), body.displayName()));
    return ResponseEntity.accepted().build();
  }
}
