package com.nm.fragmentsclean.userApplicationContext.pass.adapters.primary;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.userApplicationContext.pass.application.GetPassQuery;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/entitlements")
public class ReadPassController {
    private final QueryBus queryBus;

    public ReadPassController(QueryBus queryBus) { this.queryBus = queryBus; }

    @GetMapping
    public ResponseEntity<PassResponse> get(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return ResponseEntity.status(401).build();
        }
        PassSnapshot pass = queryBus.dispatch(new GetPassQuery(UUID.fromString(jwt.getSubject())));
        return ResponseEntity.ok(PassResponse.from(pass));
    }

    public record PassResponse(
            UUID userId,
            int confirmedTickets,
            int publishedComments,
            int confirmedLikes,
            List<String> rights,
            PassLevel currentLevel,
            PassCounters counters,
            List<LevelResponse> levels,
            Set<PassLevel> acquiredLevels,
            int policyVersion,
            long version,
            Instant updatedAt,
            Instant serverTime) {
        static PassResponse from(PassSnapshot pass) {
            return new PassResponse(pass.userId(), pass.counters().validatedTickets(), 0, 0, List.of(),
                    pass.currentLevel(), pass.counters(), pass.levels().stream().map(LevelResponse::from).toList(),
                    pass.acquiredLevels(), pass.policyVersion(), pass.version(), pass.updatedAt(), Instant.now());
        }
    }

    public record LevelResponse(PassLevel level, PassLevelStatus status,
                                PassRequirements requirements, List<String> unlockedCapabilities) {
        static LevelResponse from(PassLevelView view) {
            return new LevelResponse(view.level(), view.status(), view.requirements(), List.of());
        }
    }
}
