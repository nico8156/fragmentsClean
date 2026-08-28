package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

	public SubmitStudioArticle(
			ArticleAuthoringPort articleAuthoringPort,
			UuidGenerator uuidGenerator,
			DateTimeProvider dateTimeProvider) {
		this.articleAuthoringPort = articleAuthoringPort;
		this.uuidGenerator = uuidGenerator;
		this.dateTimeProvider = dateTimeProvider;
	}

	public StudioArticleCreationResult execute(StudioArticleSubmission submission) {
		UUID saveCommandId = uuidGenerator.generate();
		UUID reviewCommandId = uuidGenerator.generate();
		UUID publishCommandId = uuidGenerator.generate();
		UUID articleId = submission.articleId() == null ? uuidGenerator.generate() : submission.articleId();
		UUID revisionId = submission.revisionId() == null ? uuidGenerator.generate() : submission.revisionId();
		Instant now = dateTimeProvider.now();
		var cover = submission.cover();

		var command = new StudioArticleCommand(
				saveCommandId,
				now,
				articleId,
				revisionId,
				requireText(submission.slug(), "slug"),
				blankToDefault(submission.locale(), "fr-FR"),
				requireUuid(submission.authorId(), "authorId"),
				requireText(submission.authorName(), "authorName"),
				requireText(submission.title(), "title"),
				requireText(submission.intro(), "intro"),
				submission.blocks() == null ? java.util.List.of() : submission.blocks(),
				requireText(submission.conclusion(), "conclusion"),
				cover == null ? null : cover.url(),
				cover == null ? null : cover.width(),
				cover == null ? null : cover.height(),
				cover == null ? null : cover.alt(),
				submission.tags() == null ? List.of() : submission.tags(),
				submission.readingTimeMin() == null ? estimateReadingTime(submission) : submission.readingTimeMin(),
				submission.coffeeIds() == null ? List.of() : submission.coffeeIds()
		);

		articleAuthoringPort.saveDraft(command);
		articleAuthoringPort.submitForReview(reviewCommandId, now, articleId);
		articleAuthoringPort.publish(publishCommandId, now, articleId, revisionId);

		return new StudioArticleCreationResult(publishCommandId, articleId, command.slug(), command.locale(), "SUBMITTED");
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
