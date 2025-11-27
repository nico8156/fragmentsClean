package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.socialContext.read.ListCommentsQuery;
import com.nm.fragmentsclean.socialContext.read.ListCommentsQueryHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentsListView;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/social-context/comments")
public class ReadCommentsController {

    private final ListCommentsQueryHandler handler;

    public ReadCommentsController(ListCommentsQueryHandler handler) {
        this.handler = handler;
    }

    @GetMapping
    public CommentsListView listComments(
            @RequestParam("targetId") UUID targetId,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "op", defaultValue = "retrieve") String op
    ) {
        var query = new ListCommentsQuery(
                targetId,
                cursor,
                limit,
                op
        );

        return handler.handle(query);
    }
}
