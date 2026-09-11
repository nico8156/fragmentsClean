package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import java.util.UUID;

public record CommandDescriptor(
        UUID commandId,
        UUID requesterId,
        String commandType,
        String fingerprint
) {
}
