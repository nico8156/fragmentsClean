package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

/** Explicit business rejection. Technical exceptions must never be converted to this type. */
public class BusinessCommandRejectedException extends RuntimeException {
    private final String rejectionCode;

    public BusinessCommandRejectedException(String rejectionCode, String message) {
        super(message);
        this.rejectionCode = rejectionCode;
    }

    public String rejectionCode() {
        return rejectionCode;
    }
}
