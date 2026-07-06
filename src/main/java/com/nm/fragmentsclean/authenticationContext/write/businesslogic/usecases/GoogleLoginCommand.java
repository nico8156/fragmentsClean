package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record GoogleLoginCommand(
        String authorizationCode,
        String codeVerifier,
        String redirectUri,
        boolean mobilePkce
) implements Command {
    public GoogleLoginCommand(String authorizationCode) {
        this(authorizationCode, null, null, false);
    }

    public static GoogleLoginCommand mobilePkce(
            String authorizationCode,
            String codeVerifier,
            String redirectUri) {
        return new GoogleLoginCommand(authorizationCode, codeVerifier, redirectUri, true);
    }
}
