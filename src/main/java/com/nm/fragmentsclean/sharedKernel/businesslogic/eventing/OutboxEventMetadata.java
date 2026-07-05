package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

public record OutboxEventMetadata(
		String aggregateType,
		String aggregateId,
		String streamKey
) {
}
