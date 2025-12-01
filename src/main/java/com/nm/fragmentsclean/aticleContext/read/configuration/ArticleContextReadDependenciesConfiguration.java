package com.nm.fragmentsclean.aticleContext.read.configuration;


import com.nm.fragmentsclean.aticleContext.read.ListArticlesQueryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ArticleContextReadDependenciesConfiguration {
    @Bean
    ListArticlesQueryHandler listArticlesQueryHandler(
            JdbcTemplate jdbcTemplate
    ){
        return new ListArticlesQueryHandler(jdbcTemplate);
    }
}
