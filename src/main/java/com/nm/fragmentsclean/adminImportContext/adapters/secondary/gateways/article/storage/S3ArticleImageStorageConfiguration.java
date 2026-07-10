package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.storage;

import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleImageStorage;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "article.images.storage", name = "backend", havingValue = "s3")
public class S3ArticleImageStorageConfiguration {
	@Bean
	S3Client articleImageS3Client(ArticleImageStorageProperties properties) {
		S3ClientBuilder builder = S3Client.builder()
				.region(Region.of(Objects.requireNonNull(properties.getS3Region(), "article.images.storage.s3-region is required")))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
		configureCredentials(builder, properties);
		return builder.build();
	}

	@Bean
	S3Presigner articleImageS3Presigner(ArticleImageStorageProperties properties) {
		Builder builder = S3Presigner.builder()
				.region(Region.of(Objects.requireNonNull(properties.getS3Region(), "article.images.storage.s3-region is required")));
		configureCredentials(builder, properties);
		return builder.build();
	}

	@Bean
	ArticleImageStorage s3ArticleImageStorage(
			ArticleImageStorageProperties properties,
			S3Client articleImageS3Client,
			S3Presigner articleImageS3Presigner) {
		return new S3ArticleImageStorage(properties, articleImageS3Client, articleImageS3Presigner);
	}

	private void configureCredentials(S3ClientBuilder builder, ArticleImageStorageProperties properties) {
		if (properties.getS3EndpointOverride() != null) {
			builder.endpointOverride(properties.getS3EndpointOverride());
			builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
			return;
		}
		builder.credentialsProvider(DefaultCredentialsProvider.create());
	}

	private void configureCredentials(Builder builder, ArticleImageStorageProperties properties) {
		if (properties.getS3EndpointOverride() != null) {
			builder.endpointOverride(properties.getS3EndpointOverride());
			builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
			return;
		}
		builder.credentialsProvider(DefaultCredentialsProvider.create());
	}
}
