package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginCommand;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginCommandHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleLoginCommandHandler googleLoginHandler;

    public AuthController(GoogleLoginCommandHandler googleLoginHandler) {
        this.googleLoginHandler = googleLoginHandler;
    }

    @PostMapping("/google/exchange")
    public ResponseEntity<GoogleLoginResponseDto> googleExchange(@RequestBody GoogleLoginRequestDto body) {

        var result = googleLoginHandler.handle(
                new GoogleLoginCommand(
                        body.code(),
                        body.codeVerifier(),
                        body.redirectUri()
                )
        );

        var userSummary = new GoogleLoginResponseDto.UserSummary(
                result.userId(),
                result.displayName(),
                result.email(),
                result.avatarUrl()
        );

        var response = new GoogleLoginResponseDto(
                result.accessToken(),
                result.refreshToken(),
                userSummary
        );

        return ResponseEntity.ok(response);
    }
}
