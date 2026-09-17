package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync;

import java.time.Instant;
import java.util.List;

import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;

record ProjectionSyncSseEvent(
		String id,
		String eventName,
		int schemaVersion,
		String projection,
		String scope,
		String entityId,
		Long version,
		Instant changedAt,
		List<String> hints,
		String reason) {
	static ProjectionSyncSseEvent from(ProjectionSyncEvent event) {
		return new ProjectionSyncSseEvent(
				event.id(), event.eventName(), event.schemaVersion(), event.projection(), event.scope(),
				event.entityId(), event.version(), event.changedAt(), event.hints(), event.reason());
	}
}
