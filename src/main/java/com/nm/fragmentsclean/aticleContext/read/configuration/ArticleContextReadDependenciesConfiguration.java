package com.nm.fragmentsclean.aticleContext.read.configuration;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.read.GetArticleBySlugQueryHandler;
import com.nm.fragmentsclean.aticleContext.read.ListArticlesQueryHandler;
import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleCreatedEventHandler;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.JpaArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.SpringArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities")
@EnableJpaRepositories(basePackages = "com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa")
@ComponentScan(basePackages = {
        "com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class ArticleContextReadDependenciesConfiguration {

    @Bean
    @Profile("database")
    public ArticleRepository jpaArticleRepository(SpringArticleRepository springArticleRepository, ObjectMapper objectMapper){
        return new JpaArticleRepository(springArticleRepository, objectMapper);
    }

    @Bean
    CreateArticleCommandHandler createArticleCommandHandler(
            ArticleRepository articleRepository,
            DomainEventPublisher domainEventPublisher,
            DateTimeProvider dateTimeProvider){
        return new CreateArticleCommandHandler(articleRepository, domainEventPublisher, dateTimeProvider);
    }

    @Bean
    GetArticleBySlugQueryHandler getArticleBySlugQueryHandler(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper){
        return new GetArticleBySlugQueryHandler(jdbcTemplate, objectMapper);
    }

    @Bean
    ListArticlesQueryHandler listArticlesQueryHandler(JdbcTemplate jdbcTemplate, GetArticleBySlugQueryHandler getArticleBySlugQueryHandler){
        return new ListArticlesQueryHandler(jdbcTemplate, getArticleBySlugQueryHandler);
    }
    @Bean
    ArticleCreatedEventHandler articleCreatedEventHandler(ArticleProjectionRepository articleRepository){
        return new ArticleCreatedEventHandler(articleRepository);
    }

}
