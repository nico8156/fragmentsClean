package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.util.List;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminUserAccess;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;

public class ListAdminUsers {
	private final AdminUserAccessRepository repository;

	public ListAdminUsers(AdminUserAccessRepository repository) { this.repository = repository; }

	public List<AdminUserAccess> execute() { return repository.list(); }
}
