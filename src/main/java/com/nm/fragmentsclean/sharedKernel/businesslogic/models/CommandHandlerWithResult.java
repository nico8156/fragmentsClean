package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

public interface CommandHandlerWithResult<C,R> {
    R execute(C command);
}
