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
		String reason,
		ProjectionSyncAudience audience,
		String recipientId
) {
	public ProjectionSyncEvent {
		if (audience == null) {
			throw new IllegalArgumentException("Projection sync audience is required");
		}
		if (audience == ProjectionSyncAudience.USER && (recipientId == null || recipientId.isBlank())) {
			throw new IllegalArgumentException("A user projection event requires a recipient id");
		}
		if (audience != ProjectionSyncAudience.USER && recipientId != null) {
			throw new IllegalArgumentException("Only user projection events may have a recipient id");
		}
	}

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
				null,
				ProjectionSyncAudience.PUBLIC,
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
				null,
				ProjectionSyncAudience.PUBLIC,
				null);
	}

	public static ProjectionSyncEvent publicProjectionUpdated(
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
				null,
				ProjectionSyncAudience.PUBLIC,
				null);
	}

	public static ProjectionSyncEvent userProjectionUpdated(
			String recipientId,
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
				null,
				ProjectionSyncAudience.USER,
				recipientId);
	}

	public static ProjectionSyncEvent adminProjectionUpdated(
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
				null,
				ProjectionSyncAudience.ADMIN,
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
				reason,
				audience,
				recipientId);
	}
}
