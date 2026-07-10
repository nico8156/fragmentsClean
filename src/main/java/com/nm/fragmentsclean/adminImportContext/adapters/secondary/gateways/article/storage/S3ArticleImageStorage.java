package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.storage;

import java.util.Locale;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageAsset;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleImageStorage;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ArticleImageStorage implements ArticleImageStorage {
	private final ArticleImageStorageProperties properties;
	private final S3Client s3Client;

	public S3ArticleImageStorage(ArticleImageStorageProperties properties, S3Client s3Client) {
		this.properties = properties;
		this.s3Client = s3Client;
	}

	@Override
	public StudioArticleImageAsset store(UUID articleId, String fileName, String contentType, byte[] bytes, String alt) {
		if (bytes == null || bytes.length == 0) {
			throw new ArticleImageStorageException("article image bytes are required");
		}
		var bucket = requireConfigured(properties.getS3Bucket(), "article.images.storage.s3-bucket");
		var assetId = UUID.randomUUID();
		var key = keyFor(articleId, assetId, extensionFor(contentType));
		s3Client.putObject(
				PutObjectRequest.builder()
						.bucket(bucket)
						.key(key)
						.contentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType)
						.build(),
				RequestBody.fromBytes(bytes.clone()));
		return new StudioArticleImageAsset(assetId, "s3://" + bucket + "/" + key, null, null, alt);
	}

	private String keyFor(UUID articleId, UUID assetId, String extension) {
		return normalizePrefix(properties.getS3Prefix())
				+ "/"
				+ articleId
				+ "/images/"
				+ assetId
				+ extension;
	}

	private String normalizePrefix(String prefix) {
		String normalized = prefix == null || prefix.isBlank() ? "fragments/staging/articles" : prefix.trim();
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	private String extensionFor(String contentType) {
		return switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT)) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			case "image/gif" -> ".gif";
			default -> ".jpg";
		};
	}

	private String requireConfigured(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new ArticleImageStorageException(propertyName + " is required when S3 image storage is enabled");
		}
		return value.trim();
	}
}
