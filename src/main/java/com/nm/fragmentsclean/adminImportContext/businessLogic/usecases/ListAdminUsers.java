package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.util.List;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminUserAccess;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminSecurityProperties;

public class ListAdminUsers {
	private final AdminUserAccessRepository repository;
	private final AdminSecurityProperties properties;

	public ListAdminUsers(AdminUserAccessRepository repository) { this(repository, new AdminSecurityProperties()); }
	public ListAdminUsers(AdminUserAccessRepository repository, AdminSecurityProperties properties) {
		this.repository = repository; this.properties = properties;
	}

	public List<AdminUserAccess> execute() {
		var result = new java.util.ArrayList<>(repository.list());
		properties.bootstrapUserIds().forEach(id -> {
			try {
				var userId = java.util.UUID.fromString(id);
				if (result.stream().noneMatch(item -> userId.equals(item.userId()))) {
					result.add(new AdminUserAccess(userId, null, null, null, true));
				}
			} catch (IllegalArgumentException ignored) { }
		});
		properties.bootstrapEmails().forEach(email -> {
			var userId = repository.findAuthUserIdByEmail(email).orElse(null);
			if (result.stream().noneMatch(item -> email.equalsIgnoreCase(item.email()))) {
				result.add(new AdminUserAccess(userId, email, null, null, true));
			}
		});
		return result;
	}
}
