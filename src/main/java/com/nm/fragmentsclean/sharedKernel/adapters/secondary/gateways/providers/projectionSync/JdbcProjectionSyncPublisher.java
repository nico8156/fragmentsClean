package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.projectionSync;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncRepository;

@Component
public class JdbcProjectionSyncPublisher implements ProjectionSyncPublisher {
	private final ProjectionSyncRepository repository;

	public JdbcProjectionSyncPublisher(ProjectionSyncRepository repository) {
		this.repository = repository;
	}

	@Override
	public void publish(ProjectionSyncEvent event) {
		repository.append(event);
	}
}
