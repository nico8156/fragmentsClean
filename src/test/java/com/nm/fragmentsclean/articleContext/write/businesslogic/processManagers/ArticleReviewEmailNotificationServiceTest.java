package com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.articleContext.read.ArticleGenerationReviewReader;
import com.nm.fragmentsclean.articleContext.read.GetArticleGenerationReview;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewEmail;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewEmailPort;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewApprovalIssuer;

class ArticleReviewEmailNotificationServiceTest {
	@Test
	void creates_a_review_email_with_preview_and_studio_link() {
		UUID sagaId = UUID.randomUUID();
		UUID articleId = UUID.randomUUID();
		UUID revisionId = UUID.randomUUID();
		var sent = new EmailPortFake();
		ArticleReviewApprovalIssuer approvalToken = (saga, article, revision, now) -> "approval-token";
		var service = new ArticleReviewEmailNotificationService(
				new ReviewReaderFake(review(sagaId, articleId, revisionId)),
				sent,
				new ArticleReviewEmailProperties(
						true,
						"studio@anchor-event.fr",
						"nmaldiney@gmail.com",
						"https://studio-staging.anchor-event.fr",
						"eu-west-3"),
				approvalToken);

		service.notifyReviewReady(sagaId, articleId, revisionId);

		assertEquals("article-review:" + sagaId + ":" + revisionId, sent.email.idempotencyKey());
		assertTrue(sent.email.htmlBody().contains("https://studio-staging.anchor-event.fr/?articleApprovalToken=approval-token"));
		assertTrue(sent.email.htmlBody().contains("https://cdn.example.test/cover.png"));
		assertTrue(sent.email.htmlBody().contains("&lt;script&gt;"));
	}

	private static GetArticleGenerationReview review(UUID sagaId, UUID articleId, UUID revisionId) {
		return new GetArticleGenerationReview(
				sagaId,
				articleId,
				revisionId,
				"Sujet cafe",
				"READY_FOR_REVIEW",
				1,
				Instant.parse("2026-08-27T20:00:00Z"),
				new GetArticleGenerationReview.Revision(
						"Titre <script>",
						"Introduction",
						"Conclusion",
						"s3://bucket/cover.png",
						"https://cdn.example.test/cover.png",
						1536,
						1024,
						"Couverture",
						5,
						List.of("decouverte"),
						List.of(new GetArticleGenerationReview.Section(
								"Section",
								"Paragraphe",
								"s3://bucket/section.png",
								"https://cdn.example.test/section.png",
								1536,
								1024,
								"Section"))));
	}

	private record ReviewReaderFake(GetArticleGenerationReview review) implements ArticleGenerationReviewReader {
		@Override
		public GetArticleGenerationReview handle(UUID sagaId) {
			return review;
		}
	}

	private static final class EmailPortFake implements ArticleReviewEmailPort {
		private ArticleReviewEmail email;

		@Override
		public void send(ArticleReviewEmail email) {
			this.email = email;
		}
	}
}
