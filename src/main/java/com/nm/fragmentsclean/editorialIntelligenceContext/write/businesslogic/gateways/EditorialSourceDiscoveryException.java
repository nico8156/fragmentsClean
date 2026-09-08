package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

/** A categorized technical failure. The domain never receives provider exceptions or payloads. */
public final class EditorialSourceDiscoveryException extends RuntimeException {
    public enum Category { MALFORMED_ENDPOINT, MALFORMED_PAYLOAD, REMOTE_FAILURE }

    private final Category category;

    private EditorialSourceDiscoveryException(Category category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    public static EditorialSourceDiscoveryException malformedEndpoint(String message, Throwable cause) {
        return new EditorialSourceDiscoveryException(Category.MALFORMED_ENDPOINT, message, cause);
    }

    public static EditorialSourceDiscoveryException malformedPayload(String message, Throwable cause) {
        return new EditorialSourceDiscoveryException(Category.MALFORMED_PAYLOAD, message, cause);
    }

    public static EditorialSourceDiscoveryException remoteFailure(String message) {
        return new EditorialSourceDiscoveryException(Category.REMOTE_FAILURE, message, null);
    }

    public static EditorialSourceDiscoveryException remoteFailure(String message, Throwable cause) {
        return new EditorialSourceDiscoveryException(Category.REMOTE_FAILURE, message, cause);
    }

    public Category category() { return category; }
}
