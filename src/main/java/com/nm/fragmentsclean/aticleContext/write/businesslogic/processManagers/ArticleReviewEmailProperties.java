package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fragments.editorial.email")
public record ArticleReviewEmailProperties(
	boolean enabled,
	String from,
	String recipient,
	String studioBaseUrl,
	String region) {
}
