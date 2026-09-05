package com.nm.fragmentsclean.articleContext.read;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public final class GetArticleGenerationReviewQueryHandler implements ArticleGenerationReviewReader {
	private final JdbcTemplate jdbcTemplate;
	private final ArticleImageUriResolver imageUriResolver;

	public GetArticleGenerationReviewQueryHandler(
			JdbcTemplate jdbcTemplate,
			ArticleImageUriResolver imageUriResolver) {
		this.jdbcTemplate = jdbcTemplate;
		this.imageUriResolver = imageUriResolver;
	}

	@Override
	public GetArticleGenerationReview handle(UUID sagaId) {
		var saga = jdbcTemplate.queryForMap("""
				SELECT saga_id, article_id, revision_id, theme, state, generation_attempts, updated_at
				FROM article_authoring_sagas
				WHERE saga_id = ?
				""", sagaId);
		UUID articleId = (UUID) saga.get("article_id");
		UUID revisionId = (UUID) saga.get("revision_id");
		var revisions = jdbcTemplate.query("""
				SELECT title, introduction, conclusion, cover_reference, cover_width,
				       cover_height, cover_alt, reading_time_min
				FROM article_revisions
				WHERE revision_id = ?
				""", (resultSet, rowNumber) -> new GetArticleGenerationReview.Revision(
				resultSet.getString("title"),
				resultSet.getString("introduction"),
				resultSet.getString("conclusion"),
				resultSet.getString("cover_reference"),
				imageUriResolver.resolve(resultSet.getString("cover_reference")),
				(Integer) resultSet.getObject("cover_width"),
				(Integer) resultSet.getObject("cover_height"),
				resultSet.getString("cover_alt"),
				resultSet.getInt("reading_time_min"),
				tags(revisionId),
				sections(revisionId)), revisionId);

		return new GetArticleGenerationReview(
				sagaId,
				articleId,
				revisionId,
				(String) saga.get("theme"),
				(String) saga.get("state"),
				((Number) saga.get("generation_attempts")).intValue(),
				((Timestamp) saga.get("updated_at")).toInstant(),
				revisions.stream().findFirst().orElse(null));
	}

	private List<String> tags(UUID revisionId) {
		return jdbcTemplate.query("""
				SELECT tag
				FROM article_revision_tags
				WHERE revision_id = ?
				ORDER BY position
				""", (resultSet, rowNumber) -> resultSet.getString(1), revisionId);
	}

	private List<GetArticleGenerationReview.Section> sections(UUID revisionId) {
		return jdbcTemplate.query("""
				SELECT s.heading, p.body, i.storage_reference, i.width, i.height, i.alt
				FROM article_revision_sections s
				LEFT JOIN article_revision_paragraphs p ON p.section_id = s.section_id AND p.position = 0
				LEFT JOIN article_revision_images i ON i.section_id = s.section_id AND i.position = 0
				WHERE s.revision_id = ?
				ORDER BY s.position
				""", (resultSet, rowNumber) -> new GetArticleGenerationReview.Section(
				resultSet.getString("heading"),
				resultSet.getString("body"),
				resultSet.getString("storage_reference"),
				imageUriResolver.resolve(resultSet.getString("storage_reference")),
				(Integer) resultSet.getObject("width"),
				(Integer) resultSet.getObject("height"),
				resultSet.getString("alt")), revisionId);
	}
}
