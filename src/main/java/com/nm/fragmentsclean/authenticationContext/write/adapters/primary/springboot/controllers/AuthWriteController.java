package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthWriteController {


    private final CommandBus commandBus;

    public AuthWriteController(CommandBus commandBus) {
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
}
