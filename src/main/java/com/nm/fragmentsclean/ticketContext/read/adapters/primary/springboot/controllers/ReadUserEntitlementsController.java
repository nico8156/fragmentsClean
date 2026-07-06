package com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.ticketContext.read.GetUserEntitlementsQuery;
import com.nm.fragmentsclean.ticketContext.read.projections.UserEntitlementsView;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/entitlements")
public class ReadUserEntitlementsController {

    private final QueryBus queryBus;

    public ReadUserEntitlementsController(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping
    public ResponseEntity<UserEntitlementsResponse> get(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return ResponseEntity.status(401).build();
        }

        UUID userId = UUID.fromString(jwt.getSubject());
        UserEntitlementsView view = queryBus.dispatch(new GetUserEntitlementsQuery(userId));
        return ResponseEntity.ok(new UserEntitlementsResponse(
                view.userId(),
                view.confirmedTickets(),
                view.version(),
                view.updatedAt(),
                Instant.now()));
    }

    public record UserEntitlementsResponse(
            UUID userId,
            int confirmedTickets,
            long version,
            Instant updatedAt,
            Instant serverTime) {
    }
}
