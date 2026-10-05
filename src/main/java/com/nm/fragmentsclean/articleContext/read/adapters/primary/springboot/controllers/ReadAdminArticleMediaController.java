package com.nm.fragmentsclean.articleContext.read.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.articleContext.read.*;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/studio/articles/{articleId}/media")
public final class ReadAdminArticleMediaController {
    private final ListAdminArticleMediaQueryHandler queries;
    public ReadAdminArticleMediaController(ListAdminArticleMediaQueryHandler queries){this.queries=queries;}
    @GetMapping public ResponseEntity<ArticleMediaUsagePage> list(@PathVariable UUID articleId,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="30")int limit){
        return ResponseEntity.of(queries.handle(new ListAdminArticleMediaQuery(articleId,cursor,limit)));
    }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<Map<String,String>> invalid(){return ResponseEntity.badRequest().body(Map.of("error","INVALID_QUERY"));}
}
