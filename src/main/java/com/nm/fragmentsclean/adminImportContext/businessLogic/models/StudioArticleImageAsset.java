package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.UUID;

public record StudioArticleImageAsset(
		UUID assetId,
		String url,
		Integer width,
		Integer height,
		String alt) {
}
