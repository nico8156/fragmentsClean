package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.storage;

import java.net.URI;
import java.time.Duration;

import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoUriResolver;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.CoffeePhotoStorageProperties;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

public class DefaultCoffeePhotoUriResolver implements CoffeePhotoUriResolver {
	private static final String S3_SCHEME = "s3";

	private final CoffeePhotoStorageProperties properties;
	private final S3Presigner s3Presigner;

	public DefaultCoffeePhotoUriResolver(
			CoffeePhotoStorageProperties properties,
			S3Presigner s3Presigner) {
		this.properties = properties;
		this.s3Presigner = s3Presigner;
	}

	@Override
	public String resolve(String storedPhotoUri) {
		if (storedPhotoUri == null || storedPhotoUri.isBlank()) {
			return storedPhotoUri;
		}
		var trimmed = storedPhotoUri.trim();
		if (!trimmed.startsWith(S3_SCHEME + "://")) {
			return trimmed;
		}
		if (s3Presigner == null) {
			throw new IllegalStateException("S3 photo URI cannot be resolved without an S3 presigner");
		}
		var uri = URI.create(trimmed);
		var bucket = uri.getHost();
		var key = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
		if (bucket == null || bucket.isBlank() || key.isBlank()) {
			throw new IllegalArgumentException("Invalid S3 photo URI: " + trimmed);
		}
		Duration ttl = properties.getS3PresignTtl() == null ? Duration.ofMinutes(15) : properties.getS3PresignTtl();
		var request = GetObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.getObjectRequest(builder -> builder.bucket(bucket).key(key))
				.build();
		return s3Presigner.presignGetObject(request).url().toString();
	}
}
