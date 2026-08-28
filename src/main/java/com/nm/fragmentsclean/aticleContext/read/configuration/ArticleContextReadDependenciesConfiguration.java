package com.nm.fragmentsclean.aticleContext.read.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.read.GetArticleBySlugQueryHandler;
import com.nm.fragmentsclean.aticleContext.read.ListArticlesQueryHandler;
import com.nm.fragmentsclean.aticleContext.read.ArticleImageUriResolver;
import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.storage.DefaultArticleImageUriResolver;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleCreatedEventHandler;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleRevisionPublishedEventHandler;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleArchivedEventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.JpaArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.SpringArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EntityScan(basePackages = "com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities")
@EnableJpaRepositories(basePackages = "com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa")
@ComponentScan(basePackages = {
		"com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class ArticleContextReadDependenciesConfiguration {

	@Bean
	public ArticleRepository jpaArticleRepository(SpringArticleRepository springArticleRepository,
			ObjectMapper objectMapper) {
		return new JpaArticleRepository(springArticleRepository, objectMapper);
	}

	@Bean
	CreateArticleCommandHandler createArticleCommandHandler(
			ArticleRepository articleRepository,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider,
			CommandStatusRecorder commandStatusRecorder) {
		return new CreateArticleCommandHandler(articleRepository, domainEventPublisher, dateTimeProvider, commandStatusRecorder);
	}

	@Bean
	GetArticleBySlugQueryHandler getArticleBySlugQueryHandler(JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper,
			ArticleImageUriResolver articleImageUriResolver) {
		return new GetArticleBySlugQueryHandler(jdbcTemplate, objectMapper, articleImageUriResolver);
	}

	@Bean
	ListArticlesQueryHandler listArticlesQueryHandler(JdbcTemplate jdbcTemplate,
			GetArticleBySlugQueryHandler getArticleBySlugQueryHandler) {
		return new ListArticlesQueryHandler(jdbcTemplate, getArticleBySlugQueryHandler);
	}

	@Bean
	ArticleCreatedEventHandler articleCreatedEventHandler(ArticleProjectionRepository articleRepository) {
		return new ArticleCreatedEventHandler(articleRepository);
	}

	@Bean
	ArticleRevisionPublishedEventHandler articleRevisionPublishedEventHandler(
			ArticleProjectionRepository articleRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new ArticleRevisionPublishedEventHandler(articleRepository, projectionSyncPublisher);
	}

	@Bean
	ArticleArchivedEventHandler articleArchivedEventHandler(
			ArticleProjectionRepository articleRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new ArticleArchivedEventHandler(articleRepository, projectionSyncPublisher);
	}

	@Bean
	ArticleImageUriResolver articleImageUriResolver(
			ArticleImageStorageProperties properties,
			@Autowired(required = false) @Qualifier("articleImageS3Presigner") S3Presigner articleImageS3Presigner) {
		return new DefaultArticleImageUriResolver(properties, articleImageS3Presigner);
	}

}
