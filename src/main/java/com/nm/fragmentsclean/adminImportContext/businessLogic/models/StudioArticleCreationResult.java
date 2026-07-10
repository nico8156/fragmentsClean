package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.UUID;

public record StudioArticleCreationResult(
		UUID commandId,
		UUID articleId,
		String slug,
		String locale,
		String status) {
}
