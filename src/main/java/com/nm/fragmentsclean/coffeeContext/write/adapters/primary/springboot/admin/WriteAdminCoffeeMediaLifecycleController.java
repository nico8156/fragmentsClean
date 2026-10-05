package com.nm.fragmentsclean.coffeeContext.write.adapters.primary.springboot.admin;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ChangeCoffeeMediaLifecycleCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
@RestController @RequestMapping("/api/admin/studio/coffee-media") public class WriteAdminCoffeeMediaLifecycleController {
 public record Request(UUID commandId,UUID coffeeId,String status,String reason){}
 private final CommandBus commands;
 public WriteAdminCoffeeMediaLifecycleController(CommandBus commands){this.commands=commands;}
 @PostMapping("/{mediaId}/lifecycle") public ResponseEntity<Void> change(@PathVariable UUID mediaId,@RequestBody Request request,@AuthenticationPrincipal Jwt jwt){
  if(request.commandId()==null || request.coffeeId()==null)return ResponseEntity.badRequest().build();
  commands.dispatch(new ChangeCoffeeMediaLifecycleCommand(request.commandId(),request.coffeeId(),mediaId,UUID.fromString(jwt.getSubject()),request.status(),request.reason()));return ResponseEntity.accepted().build();
 }
 @PostMapping(value="/{mediaId}/replacement",consumes=org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
 public ResponseEntity<Void> replace(@PathVariable UUID mediaId,@RequestParam UUID commandId,@RequestParam UUID coffeeId,@RequestParam String reason,@RequestPart("photo") org.springframework.web.multipart.MultipartFile photo,@AuthenticationPrincipal Jwt jwt)throws java.io.IOException {
  commands.dispatch(new com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ReplaceCoffeeMediaCommand(commandId,coffeeId,mediaId,UUID.fromString(jwt.getSubject()),photo.getOriginalFilename(),photo.getContentType(),photo.getBytes(),reason));return ResponseEntity.accepted().build();
 }

}
