package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewApproval;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewApprovalRepository;

class ArticleReviewApprovalTokenServiceTest {
	@Test
	void issues_a_revision_bound_token_and_reuses_it_until_consumed() {
		var repository = new ApprovalRepositoryFake();
		var service = new ArticleReviewApprovalTokenService(
				repository,
				new ArticleReviewApprovalProperties("test-secret", Duration.ofHours(1)));
		Instant now = Instant.parse("2026-08-27T20:00:00Z");
		UUID sagaId = UUID.randomUUID();
		UUID articleId = UUID.randomUUID();
		UUID revisionId = UUID.randomUUID();

		String first = service.issue(sagaId, articleId, revisionId, now);
		String second = service.issue(sagaId, articleId, revisionId, now.plusSeconds(10));

		assertEquals(first, second);
		var approval = service.validate(first, now.plusSeconds(20));
		assertEquals(revisionId, approval.revisionId());
	}

	@Test
	void rejects_tampering_and_expired_tokens() {
		var repository = new ApprovalRepositoryFake();
		var service = new ArticleReviewApprovalTokenService(
				repository,
				new ArticleReviewApprovalProperties("test-secret", Duration.ofHours(1)));
		Instant now = Instant.parse("2026-08-27T20:00:00Z");
		String token = service.issue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), now);

		assertThrows(IllegalArgumentException.class, () -> service.validate(token + "x", now));
		assertThrows(IllegalArgumentException.class, () -> service.validate(token, now.plusSeconds(3601)));
	}

	private static final class ApprovalRepositoryFake implements ArticleReviewApprovalRepository {
		private final Map<String, ArticleReviewApproval> byHash = new HashMap<>();
		private final Map<String, ArticleReviewApproval> byRevision = new HashMap<>();

		@Override
		public Optional<ArticleReviewApproval> findBySagaAndRevision(UUID sagaId, UUID revisionId) {
			return Optional.ofNullable(byRevision.get(sagaId + ":" + revisionId));
		}

		@Override
		public Optional<ArticleReviewApproval> findByTokenHash(String tokenHash) {
			return Optional.ofNullable(byHash.get(tokenHash));
		}

		@Override
		public void save(ArticleReviewApproval approval) {
			byHash.put(approval.tokenHash(), approval);
			byRevision.put(approval.sagaId() + ":" + approval.revisionId(), approval);
		}

		@Override
		public boolean consume(UUID approvalId, Instant consumedAt) {
			return byHash.values().stream()
					.filter(approval -> approval.approvalId().equals(approvalId))
					.findFirst()
					.map(approval -> {
						if (!approval.isActiveAt(consumedAt)) {
							return false;
						}
						var consumed = new ArticleReviewApproval(
								approval.approvalId(), approval.sagaId(), approval.articleId(), approval.revisionId(),
								approval.tokenHash(), approval.createdAt(), approval.expiresAt(), consumedAt);
						byHash.put(approval.tokenHash(), consumed);
						byRevision.put(approval.sagaId() + ":" + approval.revisionId(), consumed);
						return true;
					})
					.orElse(false);
		}
	}
}
