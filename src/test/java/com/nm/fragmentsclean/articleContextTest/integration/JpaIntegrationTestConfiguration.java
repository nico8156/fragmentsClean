package com.nm.fragmentsclean.articleContextTest.integration;

import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.JpaArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.SpringArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaIntegrationTestConfiguration {
    @Bean
    public ArticleRepository articleRepository(SpringArticleRepository springArticleRepository) {
        return new JpaArticleRepository(springArticleRepository);
    }
}
