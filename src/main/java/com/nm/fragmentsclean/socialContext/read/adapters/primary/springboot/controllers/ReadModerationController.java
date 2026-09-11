package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.socialContext.read.ListBlockedUsersQueryHandler;
import com.nm.fragmentsclean.socialContext.read.ListModerationReportsQueryHandler;
import com.nm.fragmentsclean.socialContext.read.projections.BlockedUserView;
import com.nm.fragmentsclean.socialContext.read.projections.ModerationReportView;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public final class ReadModerationController {
    private final ListBlockedUsersQueryHandler blockedUsers;
    private final ListModerationReportsQueryHandler reports;
    public ReadModerationController(ListBlockedUsersQueryHandler blockedUsers, ListModerationReportsQueryHandler reports) {
        this.blockedUsers = blockedUsers; this.reports = reports;
    }
    @GetMapping("/api/social/blocks")
    List<BlockedUserView> blockedUsers(@AuthenticationPrincipal Jwt jwt) {
        return blockedUsers.handle(UUID.fromString(jwt.getSubject()));
    }
    @GetMapping("/api/admin/moderation/reports")
    List<ModerationReportView> reports(@RequestParam(defaultValue="OPEN") String status,
                                      @RequestParam(defaultValue="50") int limit) {
        return reports.handle(status, limit);
    }
}
