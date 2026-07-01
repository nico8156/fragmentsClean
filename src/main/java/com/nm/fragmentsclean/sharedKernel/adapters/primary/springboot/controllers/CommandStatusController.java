package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.CommandStatusRepository;
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

    private final CommandStatusRepository commandStatusRepository;

    public CommandStatusController(CommandStatusRepository commandStatusRepository) {
        this.commandStatusRepository = commandStatusRepository;
    }

    @GetMapping("/{commandId}")
    public ResponseEntity<CommandStatusView> getStatus(@PathVariable UUID commandId,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(commandStatusRepository.find(commandId));
    }
}
