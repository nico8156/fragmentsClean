package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleDocument;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.StudioArticleDocumentRepository;

@Repository
public class JdbcStudioArticleDocumentRepository implements StudioArticleDocumentRepository {
	private final JdbcTemplate jdbcTemplate;

	public JdbcStudioArticleDocumentRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<StudioArticleDocument> list() {
		return jdbcTemplate.query("""
				SELECT article_id, status, payload_json, created_at, updated_at, published_at, deleted_at, last_command_id
				FROM admin_studio_articles
				ORDER BY updated_at DESC
				""", this::mapRow);
	}

	@Override
	public Optional<StudioArticleDocument> findById(UUID articleId) {
		return jdbcTemplate.query("""
				SELECT article_id, status, payload_json, created_at, updated_at, published_at, deleted_at, last_command_id
				FROM admin_studio_articles
				WHERE article_id = ?
				""", this::mapRow, articleId).stream().findFirst();
	}

	@Override
	public void save(StudioArticleDocument document) {
		jdbcTemplate.update("""
				INSERT INTO admin_studio_articles (
				    article_id, status, payload_json, created_at, updated_at, published_at, deleted_at, last_command_id
				)
				VALUES (?,?,?,?,?,?,?,?)
				ON CONFLICT (article_id) DO UPDATE
				SET status = EXCLUDED.status,
				    payload_json = EXCLUDED.payload_json,
				    updated_at = EXCLUDED.updated_at,
				    published_at = EXCLUDED.published_at,
				    deleted_at = EXCLUDED.deleted_at,
				    last_command_id = EXCLUDED.last_command_id
				""",
				document.articleId(),
				document.status(),
				document.payloadJson(),
				Timestamp.from(document.createdAt()),
				Timestamp.from(document.updatedAt()),
				document.publishedAt() == null ? null : Timestamp.from(document.publishedAt()),
				document.deletedAt() == null ? null : Timestamp.from(document.deletedAt()),
				document.lastCommandId());
	}

	private StudioArticleDocument mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new StudioArticleDocument(
				rs.getObject("article_id", UUID.class),
				rs.getString("status"),
				rs.getString("payload_json"),
				rs.getTimestamp("created_at").toInstant(),
				rs.getTimestamp("updated_at").toInstant(),
				rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toInstant(),
				rs.getTimestamp("deleted_at") == null ? null : rs.getTimestamp("deleted_at").toInstant(),
				rs.getObject("last_command_id", UUID.class));
	}
}
