package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration.webSocket;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration.cors.FragmentsCorsProperties;
import java.util.List;
import java.util.Objects;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;
    private final FragmentsCorsProperties corsProperties;

    public WebSocketConfig(JwtStompChannelInterceptor jwtStompChannelInterceptor,
                           FragmentsCorsProperties corsProperties) {
        this.jwtStompChannelInterceptor = jwtStompChannelInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = clean(corsProperties.getAllowedOrigins()).toArray(String[]::new);
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
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

    private List<String> clean(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
