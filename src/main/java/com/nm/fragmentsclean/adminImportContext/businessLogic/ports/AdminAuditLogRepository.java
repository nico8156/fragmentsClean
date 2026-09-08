package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditEntry;
import java.util.List;
import java.util.UUID;

public interface AdminAuditLogRepository {
	void append(AdminAuditEntry entry);

	default List<AdminAuditEntry> findByTarget(String targetType, UUID targetId, int limit) {
		return List.of();
	}
}
