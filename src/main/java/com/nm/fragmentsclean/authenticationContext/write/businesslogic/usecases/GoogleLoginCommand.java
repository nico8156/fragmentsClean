package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record GoogleLoginCommand(
        String authorizationCode
) implements Command {
}
