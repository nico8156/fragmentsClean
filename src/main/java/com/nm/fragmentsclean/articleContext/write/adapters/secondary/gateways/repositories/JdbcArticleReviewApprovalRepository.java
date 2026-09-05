package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewApproval;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewApprovalRepository;

@Repository
public class JdbcArticleReviewApprovalRepository implements ArticleReviewApprovalRepository {
	private final JdbcTemplate jdbcTemplate;

	public JdbcArticleReviewApprovalRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<ArticleReviewApproval> findBySagaAndRevision(UUID sagaId, UUID revisionId) {
		return query("""
				SELECT approval_id, saga_id, article_id, revision_id, token_hash, created_at, expires_at, consumed_at
				FROM article_review_approvals
				WHERE saga_id = ? AND revision_id = ?
				""", sagaId, revisionId);
	}

	@Override
	public Optional<ArticleReviewApproval> findByTokenHash(String tokenHash) {
		return query("""
				SELECT approval_id, saga_id, article_id, revision_id, token_hash, created_at, expires_at, consumed_at
				FROM article_review_approvals
				WHERE token_hash = ?
				""", tokenHash);
	}

	@Override
	public void save(ArticleReviewApproval approval) {
		jdbcTemplate.update("""
				INSERT INTO article_review_approvals(
				    approval_id, saga_id, article_id, revision_id, token_hash,
				    created_at, expires_at, consumed_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				ON CONFLICT (saga_id, revision_id) DO UPDATE SET
				    approval_id = EXCLUDED.approval_id,
				    token_hash = EXCLUDED.token_hash,
				    created_at = EXCLUDED.created_at,
				    expires_at = EXCLUDED.expires_at,
				    consumed_at = EXCLUDED.consumed_at
				WHERE article_review_approvals.consumed_at IS NULL
				""",
				approval.approvalId(),
				approval.sagaId(),
				approval.articleId(),
				approval.revisionId(),
				approval.tokenHash(),
					Timestamp.from(approval.createdAt()),
					Timestamp.from(approval.expiresAt()),
					approval.consumedAt() == null ? null : Timestamp.from(approval.consumedAt()));
	}

	@Override
	public boolean consume(UUID approvalId, Instant consumedAt) {
		return jdbcTemplate.update("""
				UPDATE article_review_approvals
				SET consumed_at = ?
				WHERE approval_id = ? AND consumed_at IS NULL AND expires_at > ?
				""", Timestamp.from(consumedAt), approvalId, Timestamp.from(consumedAt)) == 1;
	}

	private Optional<ArticleReviewApproval> query(String sql, Object... arguments) {
		return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new ArticleReviewApproval(
				(UUID) resultSet.getObject("approval_id"),
				(UUID) resultSet.getObject("saga_id"),
				(UUID) resultSet.getObject("article_id"),
				(UUID) resultSet.getObject("revision_id"),
				resultSet.getString("token_hash"),
				resultSet.getTimestamp("created_at").toInstant(),
				resultSet.getTimestamp("expires_at").toInstant(),
				resultSet.getTimestamp("consumed_at") == null
						? null
						: resultSet.getTimestamp("consumed_at").toInstant()), arguments)
				.stream()
				.findFirst();
	}
}
