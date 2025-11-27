package com.nm.fragmentsclean.authContext.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto.RefreshSessionRequestDto;
import com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto.RefreshSessionResponseDto;
import com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto.mapper.RefreshSessionDtoMapper;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionCommand;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionCommandHandler;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RefreshSessionCommandHandler refreshSessionCommandHandler;

    public AuthController(RefreshSessionCommandHandler refreshSessionCommandHandler) {
        this.refreshSessionCommandHandler = refreshSessionCommandHandler;
    }

    @PostMapping("/session/refresh")
    public ResponseEntity<RefreshSessionResponseDto> refreshSession(
            @RequestBody RefreshSessionRequestDto body
    ) {
        RefreshSessionCommand command = RefreshSessionDtoMapper.toCommand(body);
        RefreshSessionResult result = refreshSessionCommandHandler.execute(command);
        RefreshSessionResponseDto response = RefreshSessionDtoMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }
}
