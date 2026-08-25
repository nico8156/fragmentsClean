package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.time.Instant;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditEntry;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditLogRepository;

public class RecordAdminAudit {
	private final AdminAuditLogRepository repository;
	public RecordAdminAudit(AdminAuditLogRepository repository) { this.repository = repository; }
	public void execute(UUID actorUserId, String action, UUID targetUserId, String outcome, Instant occurredAt) {
		repository.append(new AdminAuditEntry(UUID.randomUUID(), actorUserId, action, targetUserId, outcome, occurredAt));
	}
	public void execute(UUID actorUserId, String action, String targetType, UUID targetId,
			UUID commandId, String outcome, Instant occurredAt) {
		repository.append(new AdminAuditEntry(UUID.randomUUID(), actorUserId, action, targetType,
				targetId, commandId, outcome, occurredAt));
	}
}
