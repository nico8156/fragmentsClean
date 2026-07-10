package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageAsset;

public interface ArticleImageStorage {
	StudioArticleImageAsset store(UUID articleId, String fileName, String contentType, byte[] bytes, String alt);
}
