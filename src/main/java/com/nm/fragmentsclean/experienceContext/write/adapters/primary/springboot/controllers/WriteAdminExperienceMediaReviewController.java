package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.ReviewExperienceMediaCommand;
@RestController @RequestMapping("/api/admin/studio/experience-media")
public class WriteAdminExperienceMediaReviewController {
  public record Request(UUID commandId,Boolean approve,String reason) {}
  private final CommandBus commands;
  public WriteAdminExperienceMediaReviewController(CommandBus commands){this.commands=commands;}
  @PostMapping("/{mediaId}/review") public ResponseEntity<Void> review(@PathVariable UUID mediaId,@RequestBody Request body,@AuthenticationPrincipal Jwt jwt) {
    if(body.commandId()==null||body.approve()==null)return ResponseEntity.badRequest().build();
    commands.dispatch(new ReviewExperienceMediaCommand(body.commandId(),mediaId,UUID.fromString(jwt.getSubject()),body.approve(),body.reason()));
    return ResponseEntity.accepted().build();
  }
}
