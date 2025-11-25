package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Les destinations sur lesquelles le client s'abonne
        config.enableSimpleBroker("/topic");
        // Les destinations pour les messages émis par le client vers le serveur
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket côté serveur (ex: ws://host:port/ws)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        // .withSockJS(); // si tu veux SockJS
    }
}
