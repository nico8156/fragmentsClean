package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

public interface CommandHandler<T> {
    void execute(T command);
}
