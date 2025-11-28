package com.nm.fragmentsclean.authContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authContext.read.adapters.primary.springboot.dto.mapper.CurrentUserDtoMapper;
import com.nm.fragmentsclean.authContext.read.adapters.primary.springboot.dto.CurrentUserResponseDto;
import com.nm.fragmentsclean.authContext.read.usecases.GetCurrentUserQueryHandler;
import com.nm.fragmentsclean.authContext.read.usecases.GetCurrentUserResult;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthReadController {

    private final GetCurrentUserQueryHandler getCurrentUserQueryHandler;

    public AuthReadController(
            GetCurrentUserQueryHandler getCurrentUserQueryHandler
    ) {
        this.getCurrentUserQueryHandler = getCurrentUserQueryHandler;
    }

    // … ton POST /session/refresh et POST /session/login

    @GetMapping("/session/me")
    public ResponseEntity<CurrentUserResponseDto> getCurrentUser() {
        GetCurrentUserResult result = getCurrentUserQueryHandler.execute();
        CurrentUserResponseDto response = CurrentUserDtoMapper.toResponse(result);
        return ResponseEntity.ok(response);
    }
}
