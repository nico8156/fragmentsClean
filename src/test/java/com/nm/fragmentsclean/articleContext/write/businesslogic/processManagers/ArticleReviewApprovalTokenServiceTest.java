package com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewApproval;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewApprovalRepository;

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

	@Test
	void canonicalizes_subsecond_timestamps_without_rejecting_the_issued_token() {
		var repository = new ApprovalRepositoryFake();
		var service = new ArticleReviewApprovalTokenService(
				repository,
				new ArticleReviewApprovalProperties("test-secret", Duration.ofHours(1)));
		Instant issuedAt = Instant.parse("2026-09-17T08:00:00.123456Z");

		String token = service.issue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), issuedAt);

		var approval = service.validate(token, issuedAt.plusSeconds(1));
		assertEquals(Instant.parse("2026-09-17T08:00:00Z"), approval.createdAt());
		assertEquals(Instant.parse("2026-09-17T09:00:00Z"), approval.expiresAt());
	}

	@Test
	void accepts_a_legacy_fractional_expiry_when_its_signed_epoch_second_matches() {
		var repository = new ApprovalRepositoryFake();
		var service = new ArticleReviewApprovalTokenService(
				repository,
				new ArticleReviewApprovalProperties("test-secret", Duration.ofHours(1)));
		Instant issuedAt = Instant.parse("2026-09-17T08:00:00Z");
		String token = service.issue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), issuedAt);
		var canonical = repository.byHash.values().iterator().next();
		var legacy = new ArticleReviewApproval(
				canonical.approvalId(), canonical.sagaId(), canonical.articleId(), canonical.revisionId(),
				canonical.tokenHash(), canonical.createdAt().plusNanos(123_456_000),
				canonical.expiresAt().plusNanos(123_456_000), null);
		repository.replace(legacy);

		assertEquals(legacy, service.validate(token, issuedAt.plusSeconds(1)));
		assertThrows(IllegalArgumentException.class, () -> service.validate(token, canonical.expiresAt()));
	}

	@Test
	void replaces_an_existing_legacy_approval_during_its_unsigned_fractional_tail() {
		var repository = new ApprovalRepositoryFake();
		var service = new ArticleReviewApprovalTokenService(
				repository,
				new ArticleReviewApprovalProperties("test-secret", Duration.ofHours(1)));
		UUID sagaId = UUID.randomUUID();
		UUID articleId = UUID.randomUUID();
		UUID revisionId = UUID.randomUUID();
		Instant issuedAt = Instant.parse("2026-09-17T08:00:00Z");
		String first = service.issue(sagaId, articleId, revisionId, issuedAt);
		var canonical = repository.byHash.values().iterator().next();
		var legacy = new ArticleReviewApproval(
				canonical.approvalId(), sagaId, articleId, revisionId, canonical.tokenHash(),
				canonical.createdAt(), canonical.expiresAt().plusNanos(500_000_000), null);
		repository.replace(legacy);

		String replacement = service.issue(sagaId, articleId, revisionId, canonical.expiresAt().plusNanos(100_000_000));

		assertNotEquals(first, replacement);
		service.validate(replacement, canonical.expiresAt().plusSeconds(1));
	}

	@Test
	void rejects_a_persisted_record_not_bound_to_the_signed_saga_article_and_revision() {
		var repository = new ApprovalRepositoryFake();
		var service = new ArticleReviewApprovalTokenService(
				repository,
				new ArticleReviewApprovalProperties("test-secret", Duration.ofHours(1)));
		Instant now = Instant.parse("2026-09-17T08:00:00Z");
		String token = service.issue(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), now);
		var issued = repository.byHash.values().iterator().next();
		var wrongBindings = java.util.List.of(
				new ArticleReviewApproval(issued.approvalId(), UUID.randomUUID(), issued.articleId(),
						issued.revisionId(), issued.tokenHash(), issued.createdAt(), issued.expiresAt(), null),
				new ArticleReviewApproval(issued.approvalId(), issued.sagaId(), UUID.randomUUID(),
						issued.revisionId(), issued.tokenHash(), issued.createdAt(), issued.expiresAt(), null),
				new ArticleReviewApproval(issued.approvalId(), issued.sagaId(), issued.articleId(),
						UUID.randomUUID(), issued.tokenHash(), issued.createdAt(), issued.expiresAt(), null));

		for (var wrongBinding : wrongBindings) {
			repository.replace(wrongBinding);
			assertThrows(IllegalArgumentException.class, () -> service.validate(token, now.plusSeconds(1)));
			repository.replace(issued);
		}
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
			replace(approval);
		}

		private void replace(ArticleReviewApproval approval) {
			byRevision.values().removeIf(existing -> existing.sagaId().equals(approval.sagaId())
					&& existing.revisionId().equals(approval.revisionId()));
			byHash.values().removeIf(existing -> existing.sagaId().equals(approval.sagaId())
					&& existing.revisionId().equals(approval.revisionId()));
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
