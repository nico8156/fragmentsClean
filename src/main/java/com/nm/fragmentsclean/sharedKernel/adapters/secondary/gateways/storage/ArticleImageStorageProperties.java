package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "article.images.storage")
public class ArticleImageStorageProperties {
	private String backend = "local";
	private Path directory = Path.of("/tmp/fragments-article-images");
	private String publicBaseUrl = "";
	private String s3Bucket = "";
	private String s3Prefix = "fragments/staging/articles";
	private String s3Region = "eu-west-3";
	private URI s3EndpointOverride;
	private Duration s3PresignTtl = Duration.ofMinutes(15);

	public String getBackend() {
		return backend;
	}

	public void setBackend(String backend) {
		this.backend = backend;
	}

	public Path getDirectory() {
		return directory;
	}

	public void setDirectory(Path directory) {
		this.directory = directory;
	}

	public String getPublicBaseUrl() {
		return publicBaseUrl;
	}

	public void setPublicBaseUrl(String publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}

	public String getS3Bucket() {
		return s3Bucket;
	}

	public void setS3Bucket(String s3Bucket) {
		this.s3Bucket = s3Bucket;
	}

	public String getS3Prefix() {
		return s3Prefix;
	}

	public void setS3Prefix(String s3Prefix) {
		this.s3Prefix = s3Prefix;
	}

	public String getS3Region() {
		return s3Region;
	}

	public void setS3Region(String s3Region) {
		this.s3Region = s3Region;
	}

	public URI getS3EndpointOverride() {
		return s3EndpointOverride;
	}

	public void setS3EndpointOverride(URI s3EndpointOverride) {
		this.s3EndpointOverride = s3EndpointOverride;
	}

	public Duration getS3PresignTtl() {
		return s3PresignTtl;
	}

	public void setS3PresignTtl(Duration s3PresignTtl) {
		this.s3PresignTtl = s3PresignTtl;
	}
}
