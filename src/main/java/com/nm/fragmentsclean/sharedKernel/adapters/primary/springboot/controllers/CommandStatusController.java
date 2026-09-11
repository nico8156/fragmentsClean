package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.GetCommandStatusQuery;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusView;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/commands")
public class CommandStatusController {

    private final QueryBus queryBus;

    public CommandStatusController(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @GetMapping("/{commandId}")
    public ResponseEntity<CommandStatusView> getStatus(@PathVariable UUID commandId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(queryBus.dispatch(
                new GetCommandStatusQuery(commandId, UUID.fromString(jwt.getSubject()))));
    }
}
