package com.nm.fragmentsclean.authenticationContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authenticationContext.read.GetMeQuery;
import com.nm.fragmentsclean.authenticationContext.read.projections.AuthMeView;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthReadController {

    private final QueryBus queryBus;

    public AuthReadController(
                              QueryBus queryBus) {

        this.queryBus = queryBus;
    }
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        AuthMeView view = queryBus.dispatch(new GetMeQuery(userId));
        if (view == null) {
            return ResponseEntity.notFound().build();
        }

        MeResponse response = new MeResponse(
                view.userId(),
                jwt.getIssuedAt(),
                jwt.getExpiresAt(),
                Instant.now()
        );

        return ResponseEntity.ok(response);
    }
}
