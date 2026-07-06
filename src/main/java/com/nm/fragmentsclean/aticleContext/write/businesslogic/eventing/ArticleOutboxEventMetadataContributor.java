package com.nm.fragmentsclean.aticleContext.write.businesslogic.eventing;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataContributor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ArticleOutboxEventMetadataContributor implements OutboxEventMetadataContributor {
	@Override
	public Optional<OutboxEventMetadata> resolve(DomainEvent event) {
		if (event instanceof ArticleCreatedEvent articleEvent) {
			return Optional.of(aggregate("Article", articleEvent.articleId().toString(), "article"));
		}
		return Optional.empty();
	}

	private OutboxEventMetadata aggregate(String aggregateType, String aggregateId, String streamPrefix) {
		return new OutboxEventMetadata(aggregateType, aggregateId, streamPrefix + ":" + aggregateId);
	}
}
