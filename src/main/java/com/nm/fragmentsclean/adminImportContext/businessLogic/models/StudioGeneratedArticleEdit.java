package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.List;
import java.util.UUID;

public record StudioGeneratedArticleEdit(
		UUID sagaId,
		UUID articleId,
		UUID revisionId,
		String title,
		String introduction,
		String conclusion,
		Cover cover,
		List<Section> sections,
		List<String> tags) {
	public record Cover(String storageReference, int width, int height, String alt) {
	}

	public record Section(String heading, String paragraph, String storageReference, int width, int height, String alt) {
	}
}
