package com.nm.fragmentsclean.platform.eventing;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataContributor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataResolver;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CompositeOutboxEventMetadataResolver implements OutboxEventMetadataResolver {
	private static final Logger log = LoggerFactory.getLogger(CompositeOutboxEventMetadataResolver.class);

	private final List<OutboxEventMetadataContributor> contributors;

	public CompositeOutboxEventMetadataResolver(List<OutboxEventMetadataContributor> contributors) {
		this.contributors = List.copyOf(contributors);
	}

	@Override
	public OutboxEventMetadata resolve(DomainEvent event) {
		return contributors.stream()
				.map(contributor -> contributor.resolve(event))
				.flatMap(Optional::stream)
				.findFirst()
				.orElseGet(() -> unknown(event));
	}

	private OutboxEventMetadata unknown(DomainEvent event) {
		log.warn("Persisting domain event of unknown type in outbox type={}", event.getClass().getName());
		return new OutboxEventMetadata("Unknown", "unknown", "global");
	}
}
