package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

/** An ownerless legacy receipt cannot safely be claimed by a mobile requester. */
public class LegacyCommandReceiptUnavailableException extends RuntimeException {
    public LegacyCommandReceiptUnavailableException() {
        super("Legacy command receipt has no verifiable owner");
    }
}
