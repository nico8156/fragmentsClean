package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.ses;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleReviewEmailProperties;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleReviewApprovalProperties;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@EnableConfigurationProperties({ ArticleReviewEmailProperties.class, ArticleReviewApprovalProperties.class })
public class SesArticleReviewEmailConfiguration {
	@Bean
	@ConditionalOnProperty(name = "fragments.editorial.email.enabled", havingValue = "true")
	SesClient sesClient(ArticleReviewEmailProperties properties) {
		return SesClient.builder().region(Region.of(properties.region())).build();
	}
}
