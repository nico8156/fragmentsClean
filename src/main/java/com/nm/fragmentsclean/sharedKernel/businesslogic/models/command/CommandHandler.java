package com.nm.fragmentsclean.sharedKernel.businesslogic.models.command;


public interface CommandHandler<T> {
    void execute(T command);
}
