package com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.ticketContext.read.GetTicketHistoryQuery;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistoryPageView;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/tickets")
public final class ReadTicketHistoryController {
    private final QueryBus queryBus;

    public ReadTicketHistoryController(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping
    public ResponseEntity<TicketHistoryPageView> history(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(queryBus.dispatch(new GetTicketHistoryQuery(
                UUID.fromString(jwt.getSubject()), cursor, limit)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalidQuery(IllegalArgumentException invalid) {
        return ResponseEntity.badRequest().body(Map.of("error", "INVALID_TICKET_HISTORY_QUERY"));
    }
}
