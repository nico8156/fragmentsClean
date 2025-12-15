package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration.webSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;

    public WebSocketConfig(JwtStompChannelInterceptor jwtStompChannelInterceptor) {
        this.jwtStompChannelInterceptor = jwtStompChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // RN: websocket natif OK, SockJS non requis
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        ;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // destinations broker
        registry.enableSimpleBroker("/topic", "/queue");
        // app destinations (si un jour tu ajoutes des @MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");
        // IMPORTANT: prefix des user destinations
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}
