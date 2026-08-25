package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditEntry;

public interface AdminAuditLogRepository {
	void append(AdminAuditEntry entry);
}
