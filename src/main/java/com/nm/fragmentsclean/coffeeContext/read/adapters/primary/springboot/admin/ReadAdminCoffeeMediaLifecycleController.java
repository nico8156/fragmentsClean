package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.admin;
import com.nm.fragmentsclean.coffeeContext.read.*;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/studio/coffee-media") public class ReadAdminCoffeeMediaLifecycleController {
 private final ReadCoffeeMediaLifecycleQueryHandler queries;
 public ReadAdminCoffeeMediaLifecycleController(ReadCoffeeMediaLifecycleQueryHandler queries){this.queries=queries;}
 @GetMapping("/{mediaId}") public ResponseEntity<CoffeeMediaLifecycleView> byId(@PathVariable UUID mediaId){return ResponseEntity.of(queries.handle(new ReadCoffeeMediaLifecycleQuery(mediaId)));}
}
