package com.nm.fragmentsclean.userApplicationContext.read.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.userApplicationContext.read.*;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/studio/avatar-media")
public class ReadAdminAvatarMediaLifecycleController {
  private final ReadAvatarMediaLifecycleQueryHandler query;
  public ReadAdminAvatarMediaLifecycleController(ReadAvatarMediaLifecycleQueryHandler query){this.query=query;}
  @GetMapping("/{mediaId}") public ResponseEntity<AvatarMediaLifecycleView> byId(@PathVariable UUID mediaId){return ResponseEntity.of(query.handle(new ReadAvatarMediaLifecycleQuery(mediaId)));}
}
