package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.Authentication;
import java.time.Instant;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlacesGatewayException;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.GooglePlaceNotFoundException;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RecordAdminAudit;

@RestControllerAdvice(assignableTypes = {
		AdminImportPlacesController.class,
		AdminAccessController.class,
		AdminStudioArticlesController.class,
		AdminCommandStatusController.class
})
public class AdminImportExceptionHandler {
	private final RecordAdminAudit audit;
	public AdminImportExceptionHandler(RecordAdminAudit audit) { this.audit = audit; }
	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<AdminImportErrorResponse> badRequest(IllegalArgumentException exception, Authentication authentication) {
		record(authentication, "ADMIN_REQUEST_REJECTED", "REJECTED", exception);
		return ResponseEntity.badRequest().body(new AdminImportErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	ResponseEntity<AdminImportErrorResponse> conflict(IllegalStateException exception, Authentication authentication) {
		record(authentication, "ADMIN_REQUEST_REJECTED", "REJECTED", exception);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new AdminImportErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(GooglePlaceNotFoundException.class)
	ResponseEntity<AdminImportErrorResponse> notFound(GooglePlaceNotFoundException exception, Authentication authentication) {
		record(authentication, "ADMIN_REQUEST_REJECTED", "REJECTED", exception);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AdminImportErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(GooglePlacesGatewayException.class)
	ResponseEntity<AdminImportErrorResponse> googleGatewayError(GooglePlacesGatewayException exception, Authentication authentication) {
		record(authentication, "ADMIN_EXTERNAL_SERVICE_FAILED", "FAILED", exception);
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new AdminImportErrorResponse(exception.getMessage()));
	}

	private void record(Authentication authentication, String action, String outcome, Exception exception) {
		if (authentication == null) return;
		audit.failure(UUID.fromString(authentication.getName()), action, "REQUEST", null, null,
				outcome, exception.getClass().getSimpleName() + ": " + exception.getMessage(), Instant.now());
	}
}
