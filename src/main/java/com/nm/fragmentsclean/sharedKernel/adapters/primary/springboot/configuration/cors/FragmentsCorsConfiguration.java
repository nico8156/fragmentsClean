package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration.cors;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(FragmentsCorsProperties.class)
public class FragmentsCorsConfiguration {
	@Bean
	public CorsConfigurationSource corsConfigurationSource(FragmentsCorsProperties properties) {
		var configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(clean(properties.getAllowedOrigins()));
		configuration.setAllowedMethods(clean(properties.getAllowedMethods()));
		configuration.setAllowedHeaders(clean(properties.getAllowedHeaders()));
		configuration.setExposedHeaders(clean(properties.getExposedHeaders()));
		configuration.setAllowCredentials(properties.isAllowCredentials());
		configuration.setMaxAge(properties.getMaxAge());

		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
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
