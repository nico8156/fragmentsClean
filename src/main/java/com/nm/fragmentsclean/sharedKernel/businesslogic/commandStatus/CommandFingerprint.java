package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;

/** Secondary port isolating canonical serialization and hashing. */
public interface CommandFingerprint {
    String fingerprint(AuthenticatedCommand command);
}
