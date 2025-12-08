package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {


    private final CommandBus commandBus;

    public AuthController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @PostMapping("/google/exchange")
    public ResponseEntity<GoogleLoginResponseDto> googleExchange(@RequestBody GoogleLoginRequestDto body) {

        GoogleLoginCommand command = new GoogleLoginCommand(
                body.code(),
                body.codeVerifier(),
                body.redirectUri()
        );

        GoogleLoginResult result = commandBus.dispatchWithResult(command);

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

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDto> refresh(@RequestBody RefreshTokenRequestDto body) {

        var command = new RefreshTokenCommand(body.refreshToken());
        RefreshTokenResult result = commandBus.dispatchWithResult(command);

        var response = new RefreshTokenResponseDto(
                result.accessToken(),
                result.refreshToken()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        MeResponse response = new MeResponse(
                userId,
                jwt.getIssuedAt(),
                jwt.getExpiresAt(),
                Instant.now()
        );

        return ResponseEntity.ok(response);
    }

    public record MeResponse(
            UUID userId,
            Instant issuedAt,
            Instant expiresAt,
            Instant serverTime
    ) {}
}
