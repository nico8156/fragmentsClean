package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.util.Optional;

public interface OutboxEventMetadataContributor {
	Optional<OutboxEventMetadata> resolve(DomainEvent event);
}
