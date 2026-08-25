package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditEntry(UUID id, UUID actorUserId, String action, UUID targetUserId,
		String outcome, Instant occurredAt) { }
