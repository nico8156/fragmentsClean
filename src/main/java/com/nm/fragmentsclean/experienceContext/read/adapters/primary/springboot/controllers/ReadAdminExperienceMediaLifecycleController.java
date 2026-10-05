package com.nm.fragmentsclean.experienceContext.read.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.experienceContext.read.*;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/studio/experience-media")
public class ReadAdminExperienceMediaLifecycleController {
    private final ReadExperienceMediaLifecycleQueryHandler queries;
    public ReadAdminExperienceMediaLifecycleController(ReadExperienceMediaLifecycleQueryHandler queries) { this.queries=queries; }
    @GetMapping("/{mediaId}") public ResponseEntity<ExperienceMediaLifecycleView> byId(@PathVariable UUID mediaId) {
        return ResponseEntity.of(queries.handle(new ReadExperienceMediaLifecycleQuery(mediaId)));
    }
}
