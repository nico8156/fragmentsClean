package com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync;

public interface ProjectionSyncPublisher {
	void publish(ProjectionSyncEvent event);
}
