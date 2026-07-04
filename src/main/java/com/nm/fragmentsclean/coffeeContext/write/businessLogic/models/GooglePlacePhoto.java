package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

public record GooglePlacePhoto(String sourceName, String contentType, byte[] bytes) {
	public GooglePlacePhoto {
		if (sourceName == null || sourceName.isBlank()) {
			throw new IllegalArgumentException("sourceName is required");
		}
		sourceName = sourceName.trim();
		contentType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType.trim();
		bytes = bytes == null ? new byte[0] : bytes.clone();
		if (bytes.length == 0) {
			throw new IllegalArgumentException("photo bytes are required");
		}
	}

	@Override
	public byte[] bytes() {
		return bytes.clone();
	}
}
