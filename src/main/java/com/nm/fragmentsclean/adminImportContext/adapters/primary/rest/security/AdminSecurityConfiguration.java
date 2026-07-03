package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(AdminSecurityProperties.class)
public class AdminSecurityConfiguration {
	@Bean
	@Order(-1)
	public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
			AdminSecurityProperties properties) throws Exception {
		return http
				.securityMatcher("/api/admin/**")
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(adminCorsConfigurationSource()))
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/api/admin/**").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(new AdminTokenAuthenticationFilter(properties), UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	public CorsConfigurationSource adminCorsConfigurationSource() {
		var configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(
				"http://localhost:5173",
				"http://127.0.0.1:5173"
		));
		configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(
				HttpHeaders.AUTHORIZATION,
				HttpHeaders.CONTENT_TYPE,
				HttpHeaders.ACCEPT
		));
		configuration.setAllowCredentials(false);

		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/admin/**", configuration);
		return source;
	}
}
