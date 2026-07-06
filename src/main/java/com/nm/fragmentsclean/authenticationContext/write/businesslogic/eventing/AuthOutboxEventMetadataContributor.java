package com.nm.fragmentsclean.authenticationContext.write.businesslogic.eventing;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserLoggedInEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataContributor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthOutboxEventMetadataContributor implements OutboxEventMetadataContributor {
	@Override
	public Optional<OutboxEventMetadata> resolve(DomainEvent event) {
		if (event instanceof AuthUserCreatedEvent authEvent) {
			return Optional.of(aggregate("AuthUser", authEvent.authUserId().toString(), "authUser"));
		}
		if (event instanceof AuthUserLoggedInEvent authEvent) {
			return Optional.of(aggregate("AuthUser", authEvent.authUserId().toString(), "authUser"));
		}
		return Optional.empty();
	}

	private OutboxEventMetadata aggregate(String aggregateType, String aggregateId, String streamPrefix) {
		return new OutboxEventMetadata(aggregateType, aggregateId, streamPrefix + ":" + aggregateId);
	}
}
