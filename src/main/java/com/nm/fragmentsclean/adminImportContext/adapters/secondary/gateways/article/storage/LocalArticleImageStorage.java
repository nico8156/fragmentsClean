package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageAsset;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleImageStorage;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;

@Component
@ConditionalOnProperty(prefix = "article.images.storage", name = "backend", havingValue = "local", matchIfMissing = true)
public class LocalArticleImageStorage implements ArticleImageStorage {
	private static final String PUBLIC_PATH = "/api/articles/image-assets/";

	private final ArticleImageStorageProperties properties;

	public LocalArticleImageStorage(ArticleImageStorageProperties properties) {
		this.properties = properties;
	}

	@Override
	public StudioArticleImageAsset store(UUID articleId, String fileName, String contentType, byte[] bytes, String alt) {
		if (bytes == null || bytes.length == 0) {
			throw new ArticleImageStorageException("article image bytes are required");
		}
		var assetId = UUID.randomUUID();
		var storedFileName = assetId + extensionFor(contentType);
		var target = properties.getDirectory().resolve(storedFileName).normalize();
		if (!target.startsWith(properties.getDirectory().normalize())) {
			throw new ArticleImageStorageException("Invalid article image storage path");
		}

		try {
			Files.createDirectories(properties.getDirectory());
			Files.write(target, bytes.clone());
		} catch (IOException exception) {
			throw new ArticleImageStorageException("Failed to store article image", exception);
		}

		return new StudioArticleImageAsset(assetId, publicUri(storedFileName), null, null, alt);
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
