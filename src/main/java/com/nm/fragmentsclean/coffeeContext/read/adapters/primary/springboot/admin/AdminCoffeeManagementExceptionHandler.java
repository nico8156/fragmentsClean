package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.admin;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CoffeePhotoCommandException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice(assignableTypes = AdminCoffeesReadController.class)
public class AdminCoffeeManagementExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<AdminCoffeeErrorResponse> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new AdminCoffeeErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(CoffeePhotoCommandException.class)
    ResponseEntity<AdminCoffeeErrorResponse> coffeePhotoCommandError(CoffeePhotoCommandException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AdminCoffeeErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<AdminCoffeeErrorResponse> uploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new AdminCoffeeErrorResponse("Uploaded photo is too large."));
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<AdminCoffeeErrorResponse> multipartError(MultipartException exception) {
        return ResponseEntity.badRequest().body(new AdminCoffeeErrorResponse("Invalid multipart photo upload."));
    }
}
