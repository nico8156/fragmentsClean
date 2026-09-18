package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

public class InvalidRefreshTokenException extends RuntimeException {
	public InvalidRefreshTokenException() {
		super("Refresh token is invalid, expired or already used");
	}
}
