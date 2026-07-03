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

	public static ProjectionSyncEvent projectionUpdated(
			String projection,
			String scope,
			String entityId,
			Long version,
			Instant changedAt,
			List<String> hints) {
		return new ProjectionSyncEvent(
				null,
				"projection.updated",
				1,
				projection,
				scope,
				entityId,
				version,
				changedAt,
				hints == null ? List.of() : List.copyOf(hints),
				null);
	}

	public ProjectionSyncEvent withId(String id) {
		return new ProjectionSyncEvent(
				id,
				eventName,
				schemaVersion,
				projection,
				scope,
				entityId,
				version,
				changedAt,
				hints,
				reason);
	}
}
