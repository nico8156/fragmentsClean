package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;

public class AdminAccessPolicy {
	private final AdminSecurityProperties properties;
	private final AdminUserAccessRepository repository;

	public AdminAccessPolicy(AdminSecurityProperties properties, AdminUserAccessRepository repository) {
		this.properties = properties;
		this.repository = repository;
	}

	public boolean isAllowed(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return false;
		}
		String userId = authentication.getName();
		String email = null;
		if (authentication.getPrincipal() instanceof Jwt jwt) {
			email = jwt.getClaimAsString("email");
		}
		if (properties.isBootstrapUser(userId, email)) {
			return true;
		}
		try {
			return repository.isAllowed(UUID.fromString(userId), email);
		} catch (IllegalArgumentException exception) {
			return false;
		}
	}
}
