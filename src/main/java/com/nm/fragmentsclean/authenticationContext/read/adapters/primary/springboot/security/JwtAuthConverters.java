package com.nm.fragmentsclean.authenticationContext.read.adapters.primary.springboot.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public final class JwtAuthConverters {

    private JwtAuthConverters() { }

    public static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtAuthoritiesConverter());
        return converter;
    }
}
