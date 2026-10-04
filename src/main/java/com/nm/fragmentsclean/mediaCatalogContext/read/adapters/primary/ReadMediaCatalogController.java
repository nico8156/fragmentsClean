package com.nm.fragmentsclean.mediaCatalogContext.read.adapters.primary;
import com.nm.fragmentsclean.mediaCatalogContext.read.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/media")
public final class ReadMediaCatalogController {
    private final MediaCatalogQueryHandler queries;
    public ReadMediaCatalogController(MediaCatalogQueryHandler queries){this.queries=queries;}
    @GetMapping public MediaCatalogPage list(@RequestParam(defaultValue="") String q,
        @RequestParam(required=false) String origin, @RequestParam(required=false) String status,
        @RequestParam(required=false) UUID ownerId, @RequestParam(required=false) String cursor,
        @RequestParam(defaultValue="30") int limit) {
        return queries.handle(new SearchMediaCatalogQuery(q,origin,status,ownerId,cursor,limit));
    }
    @GetMapping("/{id}") public ResponseEntity<MediaCatalogView> detail(@PathVariable String id){return ResponseEntity.of(queries.byId(id));}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid(){return Map.of("error","INVALID_QUERY");}
}
