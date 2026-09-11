package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

/** Secondary port for the transaction containing business effects and APPLIED. */
public interface CommandTransaction {
    void requiresNew(Runnable work);
}
