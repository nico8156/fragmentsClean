package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.transport.SqsMessagingProperties;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@Component
@ConditionalOnProperty(name = "app.messaging.sqs.enabled", havingValue = "true")
public class SqsIntegrationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegrationEventConsumer.class);

    private final SqsClient sqsClient;
    private final SqsMessagingProperties properties;
    private final ObjectMapper objectMapper;
    private final SqsIntegrationEventRouter router;

    public SqsIntegrationEventConsumer(SqsClient sqsClient,
                                       SqsMessagingProperties properties,
                                       ObjectMapper objectMapper,
                                       SqsIntegrationEventRouter router) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.router = router;
    }

    @Scheduled(fixedDelayString = "${app.messaging.sqs.poll-delay-ms:1000}")
    public void poll() {
        properties.getQueues().forEach((destination, queueUrl) -> {
            if (queueUrl == null || queueUrl.isBlank()) {
                return;
            }
            pollQueue(queueUrl);
        });
    }

    private void pollQueue(String queueUrl) {
        var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(properties.getMaxMessages())
                .waitTimeSeconds((int) properties.getWaitTime().toSeconds())
                .visibilityTimeout((int) properties.getVisibilityTimeout().toSeconds())
                .build());

        for (Message message : response.messages()) {
            handleMessage(queueUrl, message);
        }
    }

    private void handleMessage(String queueUrl, Message message) {
        try {
            IntegrationEventEnvelope envelope = objectMapper.readValue(message.body(), IntegrationEventEnvelope.class);
            router.route(envelope);
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());
        } catch (Exception e) {
            log.error("[sqs] failed to process messageId={}", message.messageId(), e);
        }
    }
}
