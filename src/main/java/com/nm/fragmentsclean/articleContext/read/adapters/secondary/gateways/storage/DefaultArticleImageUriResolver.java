package com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.storage;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

import com.nm.fragmentsclean.articleContext.read.ArticleImageUriResolver;
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
		if (!trimmed.regionMatches(true, 0, S3_SCHEME + "://", 0, 5)) {
			return trimmed;
		}
		var uri = scopedReference(trimmed);
		if (s3Presigner == null) {
			throw new IllegalStateException("S3 article image URI cannot be resolved without an S3 presigner");
		}
		var bucket = uri.getHost();
		var key = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
		Duration ttl = properties.getS3PresignTtl() == null ? Duration.ofMinutes(15) : properties.getS3PresignTtl();
		var request = GetObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.getObjectRequest(builder -> builder.bucket(bucket).key(key))
				.build();
		return s3Presigner.presignGetObject(request).url().toString();
	}
	private URI scopedReference(String reference) {
		String bucket = properties.getS3Bucket();
		if (bucket == null || bucket.isBlank()) {
			throw new IllegalStateException("Article image storage bucket must be configured before signing");
		}
		String configuredPrefix = properties.getS3Prefix();
		String prefix = (configuredPrefix == null || configuredPrefix.isBlank()
				? "fragments/staging/articles" : configuredPrefix.trim()).replaceAll("^/+|/+$", "");
		if (prefix.isEmpty() || hasDotSegment(prefix)) {
			throw new IllegalStateException("Article image storage prefix must define a bounded scope");
		}
		URI uri;
		try {
			uri = URI.create(reference);
		} catch (IllegalArgumentException malformed) {
			throw new IllegalArgumentException("Invalid article image storage reference");
		}
		String path = uri.getPath();
		String root = "/" + prefix + "/";
		if (!S3_SCHEME.equals(uri.getScheme()) || !bucket.trim().equals(uri.getHost())
				|| uri.getUserInfo() != null || uri.getPort() != -1
				|| uri.getRawQuery() != null || uri.getRawFragment() != null
				|| path == null || !path.equals(uri.getRawPath())
				|| !path.startsWith(root) || path.length() <= root.length()
				|| path.indexOf('\\') >= 0 || hasDotSegment(path)) {
			throw new IllegalArgumentException("Article image reference is outside configured storage scope");
		}
		return uri;
	}

	private boolean hasDotSegment(String path) {
		return Arrays.stream(path.split("/", -1)).anyMatch(segment -> segment.equals(".") || segment.equals(".."));
	}

}
