package com.nm.fragmentsclean.authContext.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto.RefreshSessionRequestDto;
import com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto.RefreshSessionResponseDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final com.nm.fragmentsclean.auth.businesslogic.usecases.RefreshSessionCommandHandler refreshSessionCommandHandler;
    private final AuthDtoMapper mapper;

    public AuthController(com.nm.fragmentsclean.auth.businesslogic.usecases.RefreshSessionCommandHandler handler, AuthDtoMapper mapper) {
        this.refreshSessionCommandHandler = handler;
        this.mapper = mapper;
    }

    @PostMapping("/session/refresh")
    public RefreshSessionResponseDto refreshSession(@RequestBody RefreshSessionRequestDto request) {

        var cmd = mapper.toCommand(request);
        var result = refreshSessionCommandHandler.handle(cmd);
        return mapper.toDto(result);
    }
}
