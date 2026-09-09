package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

public final class EditorialScheduleConcurrencyException extends RuntimeException {
    public EditorialScheduleConcurrencyException() { super("Concurrent editorial schedule modification"); }
}
