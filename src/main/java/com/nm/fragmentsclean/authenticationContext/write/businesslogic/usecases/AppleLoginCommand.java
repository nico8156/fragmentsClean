package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record AppleLoginCommand(String identityToken, String authorizationCode, String displayName)
    implements Command {}
