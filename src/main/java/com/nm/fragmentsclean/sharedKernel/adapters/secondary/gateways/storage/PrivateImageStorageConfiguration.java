package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage;

import java.util.Objects;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaObjectKeys;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaUrlResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration(proxyBeanMethods = false)
@Profile("!fake")
@EnableConfigurationProperties(PrivateImageStorageProperties.class)
public class PrivateImageStorageConfiguration {
  @Bean SafeImageNormalizer safeImageNormalizer() { return new SafeImageNormalizer(); }

	@Bean PrivateMediaObjectKeys privateMediaObjectKeys(PrivateImageStorageProperties properties) {
		return new PrivateMediaObjectKeys(properties.normalizedPrefix());
	}

	@Bean PrivateMediaUrlResolver privateMediaUrlResolver(
			com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore store,
			PrivateImageStorageProperties properties) {
		return new PrivateMediaUrlResolver(store, properties.getDownloadTtl());
	}

  @Bean("privateMediaS3Client")
  S3Client privateMediaS3Client(PrivateImageStorageProperties properties) {
    var builder = S3Client.builder().region(Region.of(Objects.requireNonNull(properties.getRegion()))).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
    if (properties.getEndpointOverride() != null) {
      builder.endpointOverride(properties.getEndpointOverride());
      builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.create());
    }
    return builder.build();
  }

  @Bean("privateMediaS3Presigner")
  S3Presigner privateMediaS3Presigner(PrivateImageStorageProperties properties) {
    var builder = S3Presigner.builder().region(Region.of(properties.getRegion()));
    if (properties.getEndpointOverride() != null) {
      builder.endpointOverride(properties.getEndpointOverride());
      builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.create());
    }
    return builder.build();
  }

  @Bean
  com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore privateImageStore(
      PrivateImageStorageProperties properties,
      @Qualifier("privateMediaS3Client") S3Client client,
      @Qualifier("privateMediaS3Presigner") S3Presigner presigner,
      SafeImageNormalizer normalizer) {
    return new S3PrivateImageStore(properties, client, presigner, normalizer);
  }
}
