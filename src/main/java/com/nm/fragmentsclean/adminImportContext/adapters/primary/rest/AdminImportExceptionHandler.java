package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlacesGatewayException;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.GooglePlaceNotFoundException;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CoffeePhotoCommandException;

@RestControllerAdvice(assignableTypes = {
		AdminImportPlacesController.class,
		AdminCoffeesReadController.class
})
public class AdminImportExceptionHandler {
	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<AdminImportErrorResponse> badRequest(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(new AdminImportErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(GooglePlaceNotFoundException.class)
	ResponseEntity<AdminImportErrorResponse> notFound(GooglePlaceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AdminImportErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(CoffeePhotoCommandException.class)
	ResponseEntity<AdminImportErrorResponse> coffeePhotoCommandError(CoffeePhotoCommandException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AdminImportErrorResponse(exception.getMessage()));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	ResponseEntity<AdminImportErrorResponse> uploadTooLarge(MaxUploadSizeExceededException exception) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(new AdminImportErrorResponse("Uploaded photo is too large."));
	}

	@ExceptionHandler(MultipartException.class)
	ResponseEntity<AdminImportErrorResponse> multipartError(MultipartException exception) {
		return ResponseEntity.badRequest().body(new AdminImportErrorResponse("Invalid multipart photo upload."));
	}

	@ExceptionHandler(GooglePlacesGatewayException.class)
	ResponseEntity<AdminImportErrorResponse> googleGatewayError(GooglePlacesGatewayException exception) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new AdminImportErrorResponse(exception.getMessage()));
	}
}
