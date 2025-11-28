package com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.RefreshSessionRequestDto;
import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.RefreshSessionResponseDto;
import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.mapper.RefreshSessionDtoMapper;
import com.nm.fragmentsclean.authContext.write.businesslogic.usecases.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthWriteController {

    private final RefreshSessionCommandHandler refreshSessionCommandHandler;
    private final LoginCommandHandler loginCommandHandler;

    public AuthWriteController(RefreshSessionCommandHandler refreshSessionCommandHandler, LoginCommandHandler loginCommandHandler) {
        this.refreshSessionCommandHandler = refreshSessionCommandHandler;
        this.loginCommandHandler = loginCommandHandler;
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

    @PostMapping("/session/login")
    public ResponseEntity<RefreshSessionResponseDto> login(
            @RequestBody RefreshSessionRequestDto body
    ) {
        LoginCommand command = new LoginCommand(
                body.provider(),
                body.idToken(),
                body.scopes()
        );

        RefreshSessionResult result = loginCommandHandler.execute(command);
        RefreshSessionResponseDto response = RefreshSessionDtoMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }

}
