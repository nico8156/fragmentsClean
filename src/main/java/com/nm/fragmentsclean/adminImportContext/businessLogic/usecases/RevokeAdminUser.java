package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminSecurityProperties;

public class RevokeAdminUser {
	private final AdminUserAccessRepository repository;
	private final AdminSecurityProperties properties;

	public RevokeAdminUser(AdminUserAccessRepository repository) { this(repository, new AdminSecurityProperties()); }
	public RevokeAdminUser(AdminUserAccessRepository repository, AdminSecurityProperties properties) {
		this.repository = repository; this.properties = properties;
	}

	public void execute(UUID userId) {
		if (userId == null) throw new IllegalArgumentException("userId is required");
		var current = repository.findByUserId(userId);
		if (properties.isBootstrapUser(userId.toString(), current.map(com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminUserAccess::email).orElse(null)))
			throw new IllegalStateException("Bootstrap admin cannot be revoked");
		if (repository.count() <= 1 && !properties.hasBootstrapAdmin()) throw new IllegalStateException("At least one admin must remain");
		repository.revoke(userId);
	}
}
