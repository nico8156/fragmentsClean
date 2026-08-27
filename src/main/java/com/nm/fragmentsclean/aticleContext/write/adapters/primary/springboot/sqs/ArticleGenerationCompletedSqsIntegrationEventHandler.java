package com.nm.fragmentsclean.aticleContext.write.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.ARTICLES_EVENTS;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleReviewEmailNotificationService;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleGenerationCompletedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;

@Component
@ConditionalOnBean(ArticleReviewEmailNotificationService.class)
public final class ArticleGenerationCompletedSqsIntegrationEventHandler implements SqsIntegrationEventHandler {
	private final SqsIntegrationEventPayloadReader payloadReader;
	private final ArticleReviewEmailNotificationService notifications;

	public ArticleGenerationCompletedSqsIntegrationEventHandler(
			SqsIntegrationEventPayloadReader payloadReader,
			ArticleReviewEmailNotificationService notifications) {
		this.payloadReader = payloadReader;
		this.notifications = notifications;
	}

	@Override
	public SqsIntegrationEventRoute route() {
		return new SqsIntegrationEventRoute(ARTICLES_EVENTS, "article.generation.completed");
	}

	@Override
	public void handle(IntegrationEventEnvelope envelope) {
		var event = payloadReader.read(envelope, ArticleGenerationCompletedIntegrationEvent.class);
		notifications.notifyReviewReady(event.sagaId(), event.articleId(), event.revisionId());
	}
}
