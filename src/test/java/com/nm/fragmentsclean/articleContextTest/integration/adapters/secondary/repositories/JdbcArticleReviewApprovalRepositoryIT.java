package com.nm.fragmentsclean.articleContextTest.integration.adapters.secondary.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories.JdbcArticleReviewApprovalRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleReviewApprovalProperties;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleReviewApprovalTokenService;
import com.nm.fragmentsclean.articleContextTest.integration.AbstractJpaIntegrationTest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JdbcArticleReviewApprovalRepositoryIT extends AbstractJpaIntegrationTest {
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired PlatformTransactionManager transactionManager;

  @Test
  void round_trips_canonical_timestamps_and_consumes_once() {
    var fixture = fixture();

    String token = fixture.service.issue(
        fixture.sagaId, UUID.randomUUID(), fixture.revisionId,
        Instant.parse("2026-09-17T08:00:00.123456Z"));
    var approval = fixture.service.validate(token, Instant.parse("2026-09-17T08:00:01Z"));

    assertThat(approval.createdAt()).isEqualTo(Instant.parse("2026-09-17T08:00:00Z"));
    assertThat(approval.expiresAt()).isEqualTo(Instant.parse("2026-09-17T09:00:00Z"));
    assertThat(fixture.service.consume(approval.approvalId(), Instant.parse("2026-09-17T08:10:00Z"))).isTrue();
    assertThat(fixture.service.consume(approval.approvalId(), Instant.parse("2026-09-17T08:10:01Z"))).isFalse();
  }

  @Test
  void accepts_a_fractional_legacy_database_expiry_without_extending_the_signed_deadline() {
    var fixture = fixture();
    Instant issuedAt = Instant.parse("2026-09-17T08:00:00Z");
    String token = fixture.service.issue(fixture.sagaId, UUID.randomUUID(), fixture.revisionId, issuedAt);
    jdbcTemplate.update(
        "UPDATE article_review_approvals SET expires_at = expires_at + interval '123456 microseconds' WHERE saga_id = ?",
        fixture.sagaId);

    assertThat(fixture.service.validate(token, issuedAt.plusSeconds(1)).expiresAt())
        .isEqualTo(Instant.parse("2026-09-17T09:00:00.123456Z"));
    assertThatThrownBy(() -> fixture.service.validate(token, Instant.parse("2026-09-17T09:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Approval token has expired");
  }

  @Test
  void approval_consumption_rolls_back_with_its_publication_transaction() {
    var fixture = fixture();
    Instant issuedAt = Instant.parse("2026-09-17T08:00:00Z");
    String token = fixture.service.issue(fixture.sagaId, UUID.randomUUID(), fixture.revisionId, issuedAt);
    var approval = fixture.service.validate(token, issuedAt.plusSeconds(1));
    var transaction = new TransactionTemplate(transactionManager);

    assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
      assertThat(fixture.service.consume(approval.approvalId(), issuedAt.plusSeconds(2))).isTrue();
      throw new IllegalStateException("publication failed");
    })).isInstanceOf(IllegalStateException.class);

    assertThat(fixture.service.consume(approval.approvalId(), issuedAt.plusSeconds(3))).isTrue();
  }

  private Fixture fixture() {
    UUID sagaId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    Instant now = Instant.parse("2026-09-17T07:59:00Z");
    jdbcTemplate.update("""
        INSERT INTO article_authoring_sagas(
          saga_id, article_id, revision_id, theme, trigger, state, version,
          generation_attempts, created_at, updated_at)
        VALUES (?, ?, ?, 'HMAC test', 'MANUAL', 'READY_FOR_REVIEW', 1, 0, ?, ?)
        """, sagaId, UUID.randomUUID(), revisionId, Timestamp.from(now), Timestamp.from(now));
    var repository = new JdbcArticleReviewApprovalRepository(jdbcTemplate);
    return new Fixture(
        sagaId,
        revisionId,
        new ArticleReviewApprovalTokenService(
            repository, new ArticleReviewApprovalProperties("integration-test-secret", Duration.ofHours(1))));
  }

  private record Fixture(
      UUID sagaId,
      UUID revisionId,
      ArticleReviewApprovalTokenService service) {}
}
