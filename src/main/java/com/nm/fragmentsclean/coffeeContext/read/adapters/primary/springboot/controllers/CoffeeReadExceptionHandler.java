package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CoffeeReadController.class)
class CoffeeReadExceptionHandler {
	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<Map<String, String>> invalidCatalogueRequest(IllegalArgumentException error) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", error.getMessage()));
	}
}
