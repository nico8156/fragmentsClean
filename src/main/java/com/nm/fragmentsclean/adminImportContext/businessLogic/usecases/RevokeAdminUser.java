package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;

public class RevokeAdminUser {
	private final AdminUserAccessRepository repository;

	public RevokeAdminUser(AdminUserAccessRepository repository) { this.repository = repository; }

	public void execute(UUID userId) {
		if (userId == null) throw new IllegalArgumentException("userId is required");
		if (repository.count() <= 1) throw new IllegalStateException("At least one persisted admin must remain");
		repository.revoke(userId);
	}
}
