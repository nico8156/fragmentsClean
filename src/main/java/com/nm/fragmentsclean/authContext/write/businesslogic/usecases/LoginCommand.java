package com.nm.fragmentsclean.authContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.util.List;

public record LoginCommand(
        String provider,
        String idToken,
        List<String> scopes
) implements Command {
}
