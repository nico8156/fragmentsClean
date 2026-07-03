package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

public class GooglePlaceNotFoundException extends RuntimeException {
	public GooglePlaceNotFoundException(String googlePlaceId) {
		super("Google place not found: " + googlePlaceId);
	}
}
