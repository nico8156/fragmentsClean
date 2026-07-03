package com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync;

import java.util.List;

public interface ProjectionSyncRepository {
	ProjectionSyncEvent append(ProjectionSyncEvent event);

	List<ProjectionSyncEvent> findAfter(long lastEventId, int limit);

	long currentOffset();
}
