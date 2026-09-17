package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureJournal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccountErasureJournalProperties.class)
public class AccountErasureJournalConfiguration {
  @Bean("accountErasureJournalS3Client")
  @ConditionalOnProperty(prefix = "fragments.privacy.erasure-journal", name = "enabled", havingValue = "true")
  S3Client accountErasureJournalS3Client(AccountErasureJournalProperties properties) {
    var builder = S3Client.builder()
        .region(Region.of(properties.getRegion()))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
    if (properties.getEndpointOverride() != null) {
      builder.endpointOverride(properties.getEndpointOverride());
      builder.credentialsProvider(
          StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.create());
    }
    return builder.build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "fragments.privacy.erasure-journal", name = "enabled", havingValue = "true")
  AccountErasureJournal accountErasureJournal(
      AccountErasureJournalProperties properties,
      @Qualifier("accountErasureJournalS3Client") S3Client client,
      ObjectMapper objectMapper) {
    return new S3AccountErasureJournal(properties, client, objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean(AccountErasureJournal.class)
  AccountErasureJournal unavailableAccountErasureJournal() {
    return entry -> {
      throw new IllegalStateException(
          "Account deletion is unavailable because the independent erasure journal is disabled");
    };
  }
}
