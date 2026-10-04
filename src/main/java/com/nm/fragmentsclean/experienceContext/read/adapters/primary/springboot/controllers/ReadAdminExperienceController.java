package com.nm.fragmentsclean.experienceContext.read.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.experienceContext.read.*;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperiencePage;
import java.util.*;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") public final class ReadAdminExperienceController {
 private final AdminExperienceQueryHandler queries;
 public ReadAdminExperienceController(AdminExperienceQueryHandler queries){this.queries=queries;}
 @GetMapping("/experiences") public ExperiencePage list(@RequestParam(defaultValue="")String q,@RequestParam(required=false)UUID authorId,@RequestParam(required=false)String moderation,@RequestParam(required=false)String publication,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="30")int limit){return queries.handle(new SearchAdminExperiencesQuery(q,authorId,moderation,publication,ExperienceCursor.parse(cursor),limit));}
 @GetMapping("/experiences/{id}") public ResponseEntity<AdminExperienceViews.Detail> detail(@PathVariable UUID id){return ResponseEntity.of(queries.byId(id));}
 @GetMapping("/experiences/{id}/actions") public AdminExperienceViews.Actions actions(@PathVariable UUID id,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="30")int limit){return queries.actions(id,cursor,limit);}
 @GetMapping("/experience-media/{id}") public ResponseEntity<AdminExperienceViews.Media> media(@PathVariable UUID id){return ResponseEntity.of(queries.media(id));}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> invalid(){return Map.of("error","INVALID_QUERY");}
}
