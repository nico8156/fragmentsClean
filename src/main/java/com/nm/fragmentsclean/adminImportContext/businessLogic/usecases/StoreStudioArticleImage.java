package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageAsset;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleImageStorage;

public class StoreStudioArticleImage {
	private final ArticleImageStorage articleImageStorage;

	public StoreStudioArticleImage(ArticleImageStorage articleImageStorage) {
		this.articleImageStorage = articleImageStorage;
	}

	public StudioArticleImageAsset execute(UUID articleId, String fileName, String contentType, byte[] bytes, String alt) {
		return articleImageStorage.store(articleId, fileName, contentType, bytes, alt);
	}
}
