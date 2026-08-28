package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositories;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.GeneratedArticleRevisionRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleContent;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleImageRef;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleParagraph;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.ArticleEditorialTag;

@Repository
public class JdbcGeneratedArticleRevisionRepository implements GeneratedArticleRevisionRepository {
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public JdbcGeneratedArticleRevisionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void replace(
			UUID articleId,
			UUID revisionId,
			ArticleContent content,
			ArticleImageRef cover,
			List<ArticleEditorialTag> tags,
			Instant now) {
		int updated = jdbcTemplate.update("""
				UPDATE article_revisions
				SET title = ?, introduction = ?, conclusion = ?, cover_reference = ?,
				    cover_width = ?, cover_height = ?, cover_alt = ?, reading_time_min = ?,
				    updated_at = ?, version = version + 1
				WHERE article_id = ? AND revision_id = ? AND status = 'DRAFT'
				""",
				content.title().value(),
				content.introduction().value(),
				content.conclusion().value(),
				cover.storageReference(),
				cover.width(),
				cover.height(),
				cover.alt(),
				readingTime(content),
				Timestamp.from(now),
				articleId,
				revisionId);
		if (updated != 1) {
			throw new IllegalStateException("Generated article revision is missing or no longer editable");
		}

		jdbcTemplate.update("DELETE FROM article_revision_sections WHERE revision_id = ?", revisionId);
		jdbcTemplate.update("DELETE FROM article_revision_tags WHERE revision_id = ?", revisionId);
		insertSections(revisionId, content);
		insertTags(revisionId, tags);
		updateCompatibilityArticle(articleId, content, cover, tags, now);
	}

	private void insertSections(UUID revisionId, ArticleContent content) {
		int sectionPosition = 0;
		for (var section : content.sections()) {
			UUID sectionId = UUID.randomUUID();
			jdbcTemplate.update("""
					INSERT INTO article_revision_sections(section_id, revision_id, position, heading)
					VALUES (?, ?, ?, ?)
					""", sectionId, revisionId, sectionPosition++, section.heading());
			int paragraphPosition = 0;
			for (var paragraph : section.paragraphs()) {
				jdbcTemplate.update("""
						INSERT INTO article_revision_paragraphs(paragraph_id, section_id, position, body)
						VALUES (?, ?, ?, ?)
						""", UUID.randomUUID(), sectionId, paragraphPosition++, paragraph.value());
			}
			int imagePosition = 0;
			for (var image : section.images()) {
				jdbcTemplate.update("""
						INSERT INTO article_revision_images(
						    image_id, revision_id, section_id, position, storage_reference,
						    width, height, alt, source)
						VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
						""",
						UUID.randomUUID(),
						revisionId,
						sectionId,
						imagePosition++,
						image.storageReference(),
						image.width(),
						image.height(),
						image.alt(),
						"STUDIO");
			}
		}
	}

	private void insertTags(UUID revisionId, List<ArticleEditorialTag> tags) {
		int position = 0;
		for (var tag : tags) {
			jdbcTemplate.update("""
					INSERT INTO article_revision_tags(revision_id, position, tag)
					VALUES (?, ?, ?)
					""", revisionId, position++, tag.label());
		}
	}

	private void updateCompatibilityArticle(
			UUID articleId,
			ArticleContent content,
			ArticleImageRef cover,
			List<ArticleEditorialTag> tags,
			Instant now) {
		try {
			jdbcTemplate.update("""
					UPDATE articles
					SET title = ?, intro = ?, conclusion = ?, cover_url = ?, cover_width = ?,
					    cover_height = ?, cover_alt = ?, tags_json = ?, reading_time_min = ?,
					    updated_at = ?, version = version + 1
					WHERE article_id = ?
					""",
					content.title().value(),
					content.introduction().value(),
					content.conclusion().value(),
					cover.storageReference(),
					cover.width(),
					cover.height(),
					cover.alt(),
					objectMapper.writeValueAsString(tags.stream().map(ArticleEditorialTag::label).toList()),
					readingTime(content),
					Timestamp.from(now),
					articleId);
		} catch (JsonProcessingException error) {
			throw new IllegalStateException("Cannot serialize article tags", error);
		}
	}

	private int readingTime(ArticleContent content) {
		String paragraphs = content.sections().stream()
				.flatMap(section -> section.paragraphs().stream())
				.map(ArticleParagraph::value)
				.reduce("", (left, right) -> left + " " + right);
		String text = content.introduction().value() + " " + content.conclusion().value() + paragraphs;
		return Math.max(1, (text.trim().split("\\s+").length + 199) / 200);
	}
}
