package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;

public record ArticleReviewEmail(
		String idempotencyKey,
		String recipient,
		String subject,
		String textBody,
		String htmlBody) {
}
