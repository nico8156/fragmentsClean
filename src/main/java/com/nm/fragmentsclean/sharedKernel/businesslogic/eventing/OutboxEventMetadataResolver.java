package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

public interface OutboxEventMetadataResolver {
	OutboxEventMetadata resolve(DomainEvent event);
}
