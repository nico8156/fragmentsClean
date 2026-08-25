package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.admin;

import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AdminAuditRecorder;

@RestControllerAdvice(basePackages = {
        "com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.admin",
        "com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.controllers"})
public class AdminCommandFailureAuditAdvice {
    private final AdminAuditRecorder audit;
    public AdminCommandFailureAuditAdvice(AdminAuditRecorder audit) { this.audit = audit; }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException exception, Authentication authentication) {
        record(authentication, "ADMIN_COMMAND_REJECTED", "REJECTED", exception); return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorResponse> conflict(IllegalStateException exception, Authentication authentication) {
        record(authentication, "ADMIN_COMMAND_REJECTED", "REJECTED", exception); return ResponseEntity.status(409).body(new ErrorResponse(exception.getMessage()));
    }

    private void record(Authentication authentication, String action, String outcome, Exception exception) {
        if (authentication != null) audit.recordFailure(UUID.fromString(authentication.getName()), action, "REQUEST", null, null, outcome,
                exception.getClass().getSimpleName() + ": " + exception.getMessage(), Instant.now());
    }
    public record ErrorResponse(String message) { }
}
