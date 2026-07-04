package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.ImportedCoffeePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "coffee.photos.storage", name = "backend", havingValue = "local", matchIfMissing = true)
public class LocalCoffeePhotoStorage implements CoffeePhotoStorage {
	private static final String PUBLIC_PATH = "/api/coffees/photo-assets/";

	private final CoffeePhotoStorageProperties properties;

	public LocalCoffeePhotoStorage(CoffeePhotoStorageProperties properties) {
		this.properties = properties;
	}

	@Override
	public ImportedCoffeePhoto store(CoffeeId coffeeId, GooglePlaceId googlePlaceId, GooglePlacePhoto photo) {
		var photoId = UUID.nameUUIDFromBytes((coffeeId.value() + ":" + photo.sourceName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
		var fileName = photoId + extensionFor(photo.contentType());
		var target = properties.getDirectory().resolve(fileName).normalize();
		if (!target.startsWith(properties.getDirectory().normalize())) {
			throw new CoffeePhotoStorageException("Invalid coffee photo storage path");
		}

		try {
			Files.createDirectories(properties.getDirectory());
			Files.write(target, photo.bytes());
		} catch (IOException exception) {
			throw new CoffeePhotoStorageException("Failed to store coffee photo", exception);
		}

		return new ImportedCoffeePhoto(photoId, publicUri(fileName));
	}

	private String publicUri(String fileName) {
		var baseUrl = properties.getPublicBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			return PUBLIC_PATH + fileName;
		}
		return trimTrailingSlash(baseUrl) + PUBLIC_PATH + fileName;
	}

	private String trimTrailingSlash(String value) {
		var trimmed = value.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}

	private String extensionFor(String contentType) {
		return switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			case "image/gif" -> ".gif";
			default -> ".jpg";
		};
	}
}
