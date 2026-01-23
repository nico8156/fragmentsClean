package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@Profile("demo")
public class DemoSecurityConfiguration {

	@Bean
	@Order(0)
	public SecurityFilterChain demoSecurityFilterChain(HttpSecurity http) throws Exception {
		return http
				.securityMatcher("/**") // 👈 match tout avant la prod
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.anyRequest().permitAll())
				// 👇 important : désactive le resource server dans cette chain
				.oauth2ResourceServer(AbstractHttpConfigurer::disable)
				.build();
	}
}
