package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewApproval;

class JdbcArticleReviewApprovalRepositoryTest {
	@Test
	void persists_exactly_the_eight_approval_columns() {
		var jdbcTemplate = new RecordingJdbcTemplate();
		var repository = new JdbcArticleReviewApprovalRepository(jdbcTemplate);
		var approval = new ArticleReviewApproval(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				"token-hash",
				Instant.parse("2026-08-28T18:00:00Z"),
				Instant.parse("2026-08-29T18:00:00Z"),
				null);

		repository.save(approval);

		assertEquals(8, jdbcTemplate.arguments.length);
		assertEquals(approval.approvalId(), jdbcTemplate.arguments[0]);
		assertEquals(approval.tokenHash(), jdbcTemplate.arguments[4]);
		assertEquals(null, jdbcTemplate.arguments[7]);
	}

	private static final class RecordingJdbcTemplate extends JdbcTemplate {
		private Object[] arguments;

		@Override
		public int update(String sql, Object... arguments) {
			this.arguments = arguments;
			return 1;
		}
	}
}
