package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

public interface RefreshTokenHasher {
	String hash(String presentedToken);
}
