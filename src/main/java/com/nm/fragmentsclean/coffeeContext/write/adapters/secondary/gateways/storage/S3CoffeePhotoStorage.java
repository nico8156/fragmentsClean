package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.ImportedCoffeePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

public class S3CoffeePhotoStorage implements CoffeePhotoStorage {
	private final CoffeePhotoStorageProperties properties;
	private final S3Client s3Client;
	private final S3Presigner s3Presigner;

	public S3CoffeePhotoStorage(
			CoffeePhotoStorageProperties properties,
			S3Client s3Client,
			S3Presigner s3Presigner) {
		this.properties = properties;
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
	}

	@Override
	public ImportedCoffeePhoto store(CoffeeId coffeeId, GooglePlaceId googlePlaceId, GooglePlacePhoto photo) {
		var bucket = requireConfigured(properties.getS3Bucket(), "coffee.photos.storage.s3-bucket");
		var photoId = UUID.nameUUIDFromBytes((coffeeId.value() + ":" + photo.sourceName()).getBytes(StandardCharsets.UTF_8));
		var key = keyFor(coffeeId, photoId, extensionFor(photo.contentType()));
		s3Client.putObject(
				PutObjectRequest.builder()
						.bucket(bucket)
						.key(key)
						.contentType(photo.contentType())
						.build(),
				RequestBody.fromBytes(photo.bytes()));
		return new ImportedCoffeePhoto(photoId, signedPhotoUri(bucket, key).toString());
	}

	private URI signedPhotoUri(String bucket, String key) {
		Duration ttl = properties.getS3PresignTtl() == null ? Duration.ofMinutes(15) : properties.getS3PresignTtl();
		var request = GetObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.getObjectRequest(builder -> builder.bucket(bucket).key(key))
				.build();
		return URI.create(s3Presigner.presignGetObject(request).url().toString());
	}

	private String keyFor(CoffeeId coffeeId, UUID photoId, String extension) {
		return normalizePrefix(properties.getS3Prefix())
				+ "/"
				+ coffeeId.value()
				+ "/photos/"
				+ photoId
				+ extension;
	}

	private String normalizePrefix(String prefix) {
		String normalized = prefix == null || prefix.isBlank() ? "fragments/staging/coffees" : prefix.trim();
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
			throw new CoffeePhotoStorageException(propertyName + " is required when S3 photo storage is enabled");
		}
		return value.trim();
	}
}
