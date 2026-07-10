package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCreationResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageRef;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleSubmission;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

public class SubmitStudioArticle {
	private final ArticleAuthoringPort articleAuthoringPort;
	private final UuidGenerator uuidGenerator;
	private final DateTimeProvider dateTimeProvider;
	private final ObjectMapper objectMapper;

	public SubmitStudioArticle(
			ArticleAuthoringPort articleAuthoringPort,
			UuidGenerator uuidGenerator,
			DateTimeProvider dateTimeProvider,
			ObjectMapper objectMapper) {
		this.articleAuthoringPort = articleAuthoringPort;
		this.uuidGenerator = uuidGenerator;
		this.dateTimeProvider = dateTimeProvider;
		this.objectMapper = objectMapper;
	}

	public StudioArticleCreationResult execute(StudioArticleSubmission submission) {
		UUID commandId = uuidGenerator.generate();
		UUID articleId = uuidGenerator.generate();
		Instant now = dateTimeProvider.now();
		var cover = submission.cover();

		var command = new StudioArticleCommand(
				commandId,
				now,
				articleId,
				requireText(submission.slug(), "slug"),
				blankToDefault(submission.locale(), "fr-FR"),
				requireUuid(submission.authorId(), "authorId"),
				requireText(submission.authorName(), "authorName"),
				requireText(submission.title(), "title"),
				requireText(submission.intro(), "intro"),
				toJson(submission.blocks() == null ? List.of() : submission.blocks()),
				requireText(submission.conclusion(), "conclusion"),
				cover == null ? null : cover.url(),
				cover == null ? null : cover.width(),
				cover == null ? null : cover.height(),
				cover == null ? null : cover.alt(),
				submission.tags() == null ? List.of() : submission.tags(),
				submission.readingTimeMin() == null ? estimateReadingTime(submission) : submission.readingTimeMin(),
				submission.coffeeIds() == null ? List.of() : submission.coffeeIds()
		);

		articleAuthoringPort.createArticle(command);

		return new StudioArticleCreationResult(commandId, articleId, command.slug(), command.locale(), "SUBMITTED");
	}

	private int estimateReadingTime(StudioArticleSubmission submission) {
		int words = wordCount(submission.title())
				+ wordCount(submission.intro())
				+ wordCount(submission.conclusion());
		if (submission.blocks() != null) {
			for (var block : submission.blocks()) {
				words += wordCount(block.heading()) + wordCount(block.paragraph());
			}
		}
		return Math.max(1, (int) Math.ceil(words / 220.0));
	}

	private int wordCount(String value) {
		if (value == null || value.isBlank()) {
			return 0;
		}
		return value.trim().split("\\s+").length;
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Invalid article blocks", exception);
		}
	}

	private String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		return value.trim();
	}

	private String blankToDefault(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim();
	}

	private UUID requireUuid(UUID value, String field) {
		if (value == null) {
			throw new IllegalArgumentException(field + " is required");
		}
		return value;
	}
}
