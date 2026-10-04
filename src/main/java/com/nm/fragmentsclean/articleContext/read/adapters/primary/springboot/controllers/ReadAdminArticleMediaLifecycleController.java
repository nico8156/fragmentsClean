package com.nm.fragmentsclean.articleContext.read.adapters.primary.springboot.controllers;
import java.util.UUID;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.*;import com.nm.fragmentsclean.articleContext.read.*;
@RestController @RequestMapping("/api/admin/studio/article-media") public class ReadAdminArticleMediaLifecycleController {
 private final ReadArticleMediaLifecycleQueryHandler handler;public ReadAdminArticleMediaLifecycleController(ReadArticleMediaLifecycleQueryHandler handler){this.handler=handler;}
 @GetMapping("/{mediaId}") public ResponseEntity<ArticleMediaLifecycleView> read(@PathVariable UUID mediaId){return ResponseEntity.of(handler.handle(new ReadArticleMediaLifecycleQuery(mediaId)));}
}
