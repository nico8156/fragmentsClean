package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.WebSocketOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxEventSender;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Configuration
public class SharedKernelDependenciesConfiguration {
    @Bean
    public OutboxEventSender webSocketOutboxEventSender(SimpMessagingTemplate template,
                                                        ObjectMapper objectMapper) {
        return new WebSocketOutboxEventSender(template, objectMapper);
    }
}
