package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationMessagePublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.messaging.sqs.enabled", havingValue = "true")
public class SqsIntegrationMessagePublisher implements IntegrationMessagePublisher {

    private final SqsClient sqsClient;
    private final SqsMessagingProperties properties;
    private final ObjectMapper objectMapper;

    public SqsIntegrationMessagePublisher(SqsClient sqsClient,
                                          SqsMessagingProperties properties,
                                          ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(IntegrationEventEnvelope envelope) throws Exception {
        String queueUrl = properties.getQueues().get(envelope.destination());
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException("Missing SQS queue URL for destination " + envelope.destination());
        }

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(objectMapper.writeValueAsString(envelope))
                .messageAttributes(Map.of(
                        "eventId", stringAttribute(envelope.eventId()),
                        "eventType", stringAttribute(envelope.eventType()),
                        "eventVersion", stringAttribute(String.valueOf(envelope.eventVersion())),
                        "destination", stringAttribute(envelope.destination())
                ))
                .build());
    }

    private static MessageAttributeValue stringAttribute(String value) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value)
                .build();
    }
}
