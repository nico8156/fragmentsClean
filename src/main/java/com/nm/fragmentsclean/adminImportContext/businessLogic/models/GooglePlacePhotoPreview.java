package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.List;

public record GooglePlacePhotoPreview(
		String name,
		Integer widthPx,
		Integer heightPx,
		String temporaryPhotoUri,
		List<String> authorAttributions
) {
	public GooglePlacePhotoPreview {
		authorAttributions = authorAttributions == null ? List.of() : List.copyOf(authorAttributions);
	}
}
