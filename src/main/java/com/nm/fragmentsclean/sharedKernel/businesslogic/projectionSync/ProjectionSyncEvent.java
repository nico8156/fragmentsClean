package com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync;

import java.time.Instant;
import java.util.List;

public record ProjectionSyncEvent(
		String id,
		String eventName,
		int schemaVersion,
		String projection,
		String scope,
		String entityId,
		Long version,
		Instant changedAt,
		List<String> hints,
		String reason
) {
	public static ProjectionSyncEvent connected(Instant now) {
		return new ProjectionSyncEvent(
				null,
				"sync.connected",
				1,
				null,
				null,
				null,
				null,
				now,
				List.of(),
				null);
	}

	public static ProjectionSyncEvent heartbeat(Instant now) {
		return new ProjectionSyncEvent(
				null,
				"sync.heartbeat",
				1,
				null,
				null,
				null,
				null,
				now,
				List.of(),
				null);
	}
}
