package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminUserAccess;

public interface AdminUserAccessRepository {
	List<AdminUserAccess> list();
	Optional<AdminUserAccess> findByUserId(UUID userId);
	Optional<UUID> findAuthUserIdByEmail(String email);
	boolean isAllowed(UUID userId, String email);
	void grant(UUID userId, UUID grantedBy, Instant grantedAt);
	void revoke(UUID userId);
	long count();
}
