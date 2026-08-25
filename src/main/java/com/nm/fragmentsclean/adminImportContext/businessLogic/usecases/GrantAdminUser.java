package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.time.Instant;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminUserAccess;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;

public class GrantAdminUser {
	private final AdminUserAccessRepository repository;

	public GrantAdminUser(AdminUserAccessRepository repository) { this.repository = repository; }

	public AdminUserAccess execute(UUID userId, String email, UUID grantedBy, Instant now) {
		UUID resolvedUserId = userId;
		if (resolvedUserId == null && email != null && !email.isBlank()) {
			resolvedUserId = repository.findAuthUserIdByEmail(email.trim())
					.orElseThrow(() -> new IllegalArgumentException("No authenticated user exists for this email"));
		}
		if (resolvedUserId == null) throw new IllegalArgumentException("userId or email is required");
		repository.grant(resolvedUserId, grantedBy, now);
		return repository.findByUserId(resolvedUserId)
				.orElseThrow(() -> new IllegalStateException("Admin access was not persisted"));
	}
}
