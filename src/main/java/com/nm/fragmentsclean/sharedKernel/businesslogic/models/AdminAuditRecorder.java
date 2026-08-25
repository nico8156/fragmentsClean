package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

import java.time.Instant;
import java.util.UUID;

/** Primitive ACL used by admin adapters in other bounded contexts. */
public interface AdminAuditRecorder {
	void record(UUID actorUserId, String action, String targetType, UUID targetId,
			UUID commandId, String outcome, Instant occurredAt);
}
