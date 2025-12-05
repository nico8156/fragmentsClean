package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public interface CommandHandlerWithResult<C extends Command,R> {
    R execute(C command);
}
