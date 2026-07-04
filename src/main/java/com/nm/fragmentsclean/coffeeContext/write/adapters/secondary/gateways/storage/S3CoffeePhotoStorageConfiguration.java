package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage;

import java.util.Objects;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
@ConditionalOnProperty(prefix = "coffee.photos.storage", name = "backend", havingValue = "s3")
public class S3CoffeePhotoStorageConfiguration {

	@Bean
	S3Client coffeePhotoS3Client(CoffeePhotoStorageProperties properties) {
		S3ClientBuilder builder = S3Client.builder()
				.region(Region.of(Objects.requireNonNull(properties.getS3Region(), "coffee.photos.storage.s3-region is required")))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
		configureCredentials(builder, properties);
		return builder.build();
	}

	@Bean
	S3Presigner coffeePhotoS3Presigner(CoffeePhotoStorageProperties properties) {
		Builder builder = S3Presigner.builder()
				.region(Region.of(Objects.requireNonNull(properties.getS3Region(), "coffee.photos.storage.s3-region is required")));
		configureCredentials(builder, properties);
		return builder.build();
	}

	@Bean
	CoffeePhotoStorage s3CoffeePhotoStorage(
			CoffeePhotoStorageProperties properties,
			S3Client coffeePhotoS3Client,
			S3Presigner coffeePhotoS3Presigner) {
		return new S3CoffeePhotoStorage(properties, coffeePhotoS3Client, coffeePhotoS3Presigner);
	}

	private void configureCredentials(S3ClientBuilder builder, CoffeePhotoStorageProperties properties) {
		if (properties.getS3EndpointOverride() != null) {
			builder.endpointOverride(properties.getS3EndpointOverride());
			builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
			return;
		}
		builder.credentialsProvider(DefaultCredentialsProvider.create());
	}

	private void configureCredentials(Builder builder, CoffeePhotoStorageProperties properties) {
		if (properties.getS3EndpointOverride() != null) {
			builder.endpointOverride(properties.getS3EndpointOverride());
			builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
			return;
		}
		builder.credentialsProvider(DefaultCredentialsProvider.create());
	}
}
