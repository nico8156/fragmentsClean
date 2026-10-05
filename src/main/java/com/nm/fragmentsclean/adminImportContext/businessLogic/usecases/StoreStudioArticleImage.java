package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageAsset;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleImageStorage;

public class StoreStudioArticleImage {
	private final ArticleImageStorage articleImageStorage;
    private final com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleMediaUploadTracking tracking;

	public StoreStudioArticleImage(ArticleImageStorage articleImageStorage, com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleMediaUploadTracking tracking) {
		this.articleImageStorage = articleImageStorage;this.tracking=tracking;
	}

	public StudioArticleImageAsset execute(UUID articleId, String fileName, String contentType, byte[] bytes, String alt, UUID uploadedBy) {
		var asset=articleImageStorage.store(articleId,fileName,contentType,bytes,alt);
        tracking.record(articleId,asset.url(),fileName,contentType,bytes,asset.width(),asset.height(),uploadedBy,"STUDIO");
        return asset;
	}
}
