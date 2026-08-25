package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditEntry(UUID id, UUID actorUserId, String action, String targetType,
		UUID targetId, UUID commandId, String outcome, String reason, Instant occurredAt) {
	public AdminAuditEntry(UUID id, UUID actorUserId, String action, UUID targetUserId,
			String outcome, Instant occurredAt) {
		this(id, actorUserId, action, "USER", targetUserId, null, outcome, null, occurredAt);
	}
}
