package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.eventing;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataContributor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AppUserOutboxEventMetadataContributor implements OutboxEventMetadataContributor {
	@Override
	public Optional<OutboxEventMetadata> resolve(DomainEvent event) {
		if (event instanceof AppUserCreatedEvent appEvent) {
			return Optional.of(aggregate("AppUser", appEvent.userId().toString(), "appUser"));
		}
		if (event instanceof AppUserProfileUpdatedEvent appEvent) {
			return Optional.of(aggregate("AppUser", appEvent.userId().toString(), "appUser"));
		}
		return Optional.empty();
	}

	private OutboxEventMetadata aggregate(String aggregateType, String aggregateId, String streamPrefix) {
		return new OutboxEventMetadata(aggregateType, aggregateId, streamPrefix + ":" + aggregateId);
	}
}
