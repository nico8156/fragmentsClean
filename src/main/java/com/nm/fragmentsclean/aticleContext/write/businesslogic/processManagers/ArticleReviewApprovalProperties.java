package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fragments.editorial.approval")
public record ArticleReviewApprovalProperties(
		String secret,
		Duration ttl) {
}
