package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.controllers;
import java.util.*;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.ChangeAvatarMediaLifecycleCommand;
@RestController @RequestMapping("/api/admin/studio/avatar-media") public class WriteAdminAvatarMediaLifecycleController {
 public record Request(UUID commandId,String status,String reason){}
 private final CommandBus commands;public WriteAdminAvatarMediaLifecycleController(CommandBus commands){this.commands=commands;}
 @PostMapping("/{mediaId}/lifecycle") public ResponseEntity<Void> change(@PathVariable UUID mediaId,@RequestBody Request body,@AuthenticationPrincipal Jwt jwt){if(body.commandId()==null)return ResponseEntity.badRequest().build();commands.dispatch(new ChangeAvatarMediaLifecycleCommand(body.commandId(),mediaId,UUID.fromString(jwt.getSubject()),body.status(),body.reason()));return ResponseEntity.accepted().build();}
}
