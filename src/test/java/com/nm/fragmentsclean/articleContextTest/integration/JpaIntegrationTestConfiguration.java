package com.nm.fragmentsclean.articleContextTest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.JpaArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.SpringArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;


@TestConfiguration
public class JpaIntegrationTestConfiguration {


    @Bean
    public ArticleRepository articleRepository(SpringArticleRepository springArticleRepository, ObjectMapper objectMapper) {
        return new JpaArticleRepository(springArticleRepository, objectMapper);
    }
}
