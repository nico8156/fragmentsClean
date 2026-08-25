package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlacesGatewayException;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.GooglePlaceNotFoundException;

@RestControllerAdvice(assignableTypes = {
		AdminImportPlacesController.class,
		AdminAccessController.class
})
public class AdminImportExceptionHandler {
	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<AdminImportErrorResponse> badRequest(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(new AdminImportErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	ResponseEntity<AdminImportErrorResponse> conflict(IllegalStateException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new AdminImportErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(GooglePlaceNotFoundException.class)
	ResponseEntity<AdminImportErrorResponse> notFound(GooglePlaceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AdminImportErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(GooglePlacesGatewayException.class)
	ResponseEntity<AdminImportErrorResponse> googleGatewayError(GooglePlacesGatewayException exception) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new AdminImportErrorResponse(exception.getMessage()));
	}
}
