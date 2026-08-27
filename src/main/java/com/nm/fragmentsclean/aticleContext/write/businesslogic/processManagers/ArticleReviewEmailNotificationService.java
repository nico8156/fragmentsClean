package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.aticleContext.read.GetArticleGenerationReview;
import com.nm.fragmentsclean.aticleContext.read.ArticleGenerationReviewReader;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewEmail;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewEmailPort;

@Component
@ConditionalOnBean(ArticleReviewEmailPort.class)
public final class ArticleReviewEmailNotificationService {
	private final ArticleGenerationReviewReader reviews;
	private final ArticleReviewEmailPort emailPort;
	private final ArticleReviewEmailProperties properties;

	public ArticleReviewEmailNotificationService(
			ArticleGenerationReviewReader reviews,
			ArticleReviewEmailPort emailPort,
			ArticleReviewEmailProperties properties) {
		this.reviews = reviews;
		this.emailPort = emailPort;
		this.properties = properties;
	}

	public void notifyReviewReady(UUID sagaId, UUID articleId, UUID revisionId) {
		GetArticleGenerationReview review = reviews.handle(sagaId);
		if (!articleId.equals(review.articleId()) || !revisionId.equals(review.revisionId())) {
			throw new IllegalArgumentException("Review notification identity does not match the saga snapshot");
		}
		if (review.revision() == null) {
			throw new IllegalStateException("A review notification requires a materialized revision");
		}

		String subject = "Article prêt à relire : " + review.subject();
		String studioUrl = properties.studioBaseUrl() + "/?articleGenerationSagaId=" + sagaId;
		emailPort.send(new ArticleReviewEmail(
				"article-review:" + sagaId + ":" + revisionId,
				properties.recipient(),
				subject,
				textBody(review, studioUrl),
				htmlBody(review, studioUrl)));
	}

	private String textBody(GetArticleGenerationReview review, String studioUrl) {
		return "L'article « " + review.revision().title() + " » est prêt à relire.\n\n"
				+ "Ouvrir dans Fragments Studio : " + studioUrl + "\n\n"
				+ "Cette génération reste un brouillon jusqu'à votre validation.";
	}

	private String htmlBody(GetArticleGenerationReview review, String studioUrl) {
		var revision = review.revision();
		StringBuilder html = new StringBuilder("<html><body>");
		html.append("<p><strong>Fragments Studio</strong></p>");
		html.append("<h1>").append(escape(revision.title())).append("</h1>");
		html.append("<p>").append(escape(revision.introduction())).append("</p>");
		appendImage(html, revision.coverUrl(), revision.coverAlt());
		for (var section : revision.sections()) {
			html.append("<h2>").append(escape(section.heading())).append("</h2>");
			html.append("<p>").append(escape(section.paragraph())).append("</p>");
			appendImage(html, section.imageUrl(), section.imageAlt());
		}
		html.append("<p>").append(escape(revision.conclusion())).append("</p>");
		html.append("<p><a href=\"").append(escape(studioUrl))
				.append("\">Ouvrir et relire dans Studio</a></p>");
		html.append("<p>La publication nécessite une validation explicite dans Studio.</p>");
		html.append("</body></html>");
		return html.toString();
	}

	private void appendImage(StringBuilder html, String url, String alt) {
		if (url != null && !url.isBlank()) {
			html.append("<p><img src=\"").append(escape(url))
					.append("\" alt=\"").append(escape(alt)).append("\" width=\"640\"></p>");
		}
	}

	private String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
