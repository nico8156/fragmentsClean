package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record EditGeneratedArticleRevisionCommand(
		UUID commandId,
		Instant clientAt,
		UUID sagaId,
		UUID articleId,
		UUID revisionId,
		String title,
		String introduction,
		String conclusion,
		Cover cover,
		List<Section> sections,
		List<String> tags) implements Command {
	public record Cover(String storageReference, int width, int height, String alt) {
	}

	public record Section(String heading, String paragraph, String storageReference, int width, int height, String alt) {
	}
}
