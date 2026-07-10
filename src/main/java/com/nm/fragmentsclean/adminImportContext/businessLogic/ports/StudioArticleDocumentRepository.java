package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleDocument;

public interface StudioArticleDocumentRepository {
	List<StudioArticleDocument> list();

	Optional<StudioArticleDocument> findById(UUID articleId);

	void save(StudioArticleDocument document);
}
