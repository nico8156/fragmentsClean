package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginCommand;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginCommandHandler;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.GoogleLoginResult;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
