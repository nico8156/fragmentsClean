package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

/** A command id was reused by another requester or for another intent. */
public class CommandIdentityConflictException extends RuntimeException {
    public CommandIdentityConflictException() {
        super("Command id is already associated with another intent");
    }
}
