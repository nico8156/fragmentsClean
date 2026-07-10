package com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.storage;

import java.net.URI;
import java.time.Duration;

import com.nm.fragmentsclean.aticleContext.read.ArticleImageUriResolver;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

public class DefaultArticleImageUriResolver implements ArticleImageUriResolver {
	private static final String S3_SCHEME = "s3";

	private final ArticleImageStorageProperties properties;
	private final S3Presigner s3Presigner;

	public DefaultArticleImageUriResolver(
			ArticleImageStorageProperties properties,
			S3Presigner s3Presigner) {
		this.properties = properties;
		this.s3Presigner = s3Presigner;
	}

	@Override
	public String resolve(String storedImageUri) {
		if (storedImageUri == null || storedImageUri.isBlank()) {
			return storedImageUri;
		}
		var trimmed = storedImageUri.trim();
		if (!trimmed.startsWith(S3_SCHEME + "://")) {
			return trimmed;
		}
		if (s3Presigner == null) {
			throw new IllegalStateException("S3 article image URI cannot be resolved without an S3 presigner");
		}
		var uri = URI.create(trimmed);
		var bucket = uri.getHost();
		var key = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
		if (bucket == null || bucket.isBlank() || key.isBlank()) {
			throw new IllegalArgumentException("Invalid S3 article image URI: " + trimmed);
		}
		Duration ttl = properties.getS3PresignTtl() == null ? Duration.ofMinutes(15) : properties.getS3PresignTtl();
		var request = GetObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.getObjectRequest(builder -> builder.bucket(bucket).key(key))
				.build();
		return s3Presigner.presignGetObject(request).url().toString();
	}
}
