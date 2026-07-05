package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.springframework.stereotype.Component;

@Component
public class SqsIntegrationEventPayloadReader {

    private final ObjectMapper objectMapper;

    public SqsIntegrationEventPayloadReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T read(IntegrationEventEnvelope envelope, Class<T> eventClass) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), eventClass);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize " + envelope.eventType(), e);
        }
    }
}
