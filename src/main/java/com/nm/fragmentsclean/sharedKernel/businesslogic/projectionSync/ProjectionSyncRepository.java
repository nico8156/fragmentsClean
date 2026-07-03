package com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync;

import java.util.List;
import java.util.OptionalLong;

public interface ProjectionSyncRepository {
	ProjectionSyncEvent append(ProjectionSyncEvent event);

	List<ProjectionSyncEvent> findAfter(OptionalLong lastEventId, int limit);
}
