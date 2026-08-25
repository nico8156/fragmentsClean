package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;
import com.nm.fragmentsclean.authenticationContext.read.adapters.primary.springboot.security.JwtAuthConverters;

@Configuration
@EnableConfigurationProperties(AdminSecurityProperties.class)
public class AdminSecurityConfiguration {
	@Bean
	@Order(-1)
	public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
			AdminSecurityProperties properties,
			AdminUserAccessRepository adminUserAccessRepository) throws Exception {
		var policy = new AdminAccessPolicy(properties, adminUserAccessRepository);
		return http
				.securityMatcher("/api/admin/**")
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.anyRequest().access(new AdminAccessAuthorizationManager(policy)))
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
						.jwtAuthenticationConverter(JwtAuthConverters.jwtAuthenticationConverter())))
				.build();
	}
}
