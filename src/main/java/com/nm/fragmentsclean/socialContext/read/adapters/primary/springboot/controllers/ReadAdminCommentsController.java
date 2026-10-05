package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.socialContext.read.*;
import java.util.Map;import java.util.UUID;
import org.springframework.http.HttpStatus;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/comments") public final class ReadAdminCommentsController {
 private final SearchAdminCommentsQueryHandler queries;
 private final ListUserCommentReportsQueryHandler reports;
 private final ListUserCommentActionsQueryHandler actions;
 public ReadAdminCommentsController(SearchAdminCommentsQueryHandler queries,ListUserCommentReportsQueryHandler reports,ListUserCommentActionsQueryHandler actions){this.queries=queries;this.reports=reports;this.actions=actions;}
 @GetMapping public AdminCommentViews.Page list(@RequestParam UUID authorId,@RequestParam(required=false)String moderation,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="30")int limit){return queries.handle(new SearchAdminCommentsQuery(authorId,moderation,AdminCommentCursor.parse(cursor),limit));}
 @GetMapping("/reports") public AdminCommentViews.Reports reports(@RequestParam UUID authorId,@RequestParam(required=false)String status,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="30")int limit){return reports.handle(new ListUserCommentReportsQuery(authorId,status,AdminCommentCursor.parse(cursor),limit));}
 @GetMapping("/actions") public AdminCommentViews.Actions actions(@RequestParam UUID authorId,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="30")int limit){return actions.handle(new ListUserCommentActionsQuery(authorId,AdminCommentCursor.parse(cursor),limit));}
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public Map<String,String> invalid(){return Map.of("error","INVALID_QUERY");}
}
