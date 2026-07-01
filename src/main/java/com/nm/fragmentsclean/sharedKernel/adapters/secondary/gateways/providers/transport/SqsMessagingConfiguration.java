package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.transport;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@EnableConfigurationProperties(SqsMessagingProperties.class)
public class SqsMessagingConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.messaging.sqs.enabled", havingValue = "true")
    SqsClient sqsClient(SqsMessagingProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
