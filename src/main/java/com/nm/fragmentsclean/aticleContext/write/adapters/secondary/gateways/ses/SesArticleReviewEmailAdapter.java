package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.ses;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewEmail;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewEmailPort;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleReviewEmailProperties;

import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Component
@ConditionalOnProperty(name = "fragments.editorial.email.enabled", havingValue = "true")
public final class SesArticleReviewEmailAdapter implements ArticleReviewEmailPort {
	private final SesClient sesClient;
	private final ArticleReviewEmailProperties properties;

	public SesArticleReviewEmailAdapter(SesClient sesClient, ArticleReviewEmailProperties properties) {
		this.sesClient = sesClient;
		this.properties = properties;
	}

	@Override
	public void send(ArticleReviewEmail email) {
		sesClient.sendEmail(SendEmailRequest.builder()
				.source(properties.from())
				.destination(Destination.builder().toAddresses(email.recipient()).build())
				.message(Message.builder()
						.subject(Content.builder().data(email.subject()).charset("UTF-8").build())
						.body(Body.builder()
								.text(Content.builder().data(email.textBody()).charset("UTF-8").build())
								.html(Content.builder().data(email.htmlBody()).charset("UTF-8").build())
								.build())
						.build())
				.build());
	}
}
