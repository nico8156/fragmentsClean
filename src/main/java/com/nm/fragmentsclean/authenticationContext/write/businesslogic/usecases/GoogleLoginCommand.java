package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record GoogleLoginCommand(
        String authorizationCode,
        String codeVerifier,
        String redirectUri,
        Client client
) implements Command {
    public GoogleLoginCommand(String authorizationCode, String codeVerifier, String redirectUri) {
        this(authorizationCode, codeVerifier, redirectUri, Client.MOBILE);
    }

    public enum Client { MOBILE, STUDIO }
}
