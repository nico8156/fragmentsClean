package com.nm.fragmentsclean.adminImportContext.configuration;

import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.CommandBusArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.CommandBusArticleGenerationAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.coffee.CommandBusCoffeeCreationPort;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlacesProperties;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleGenerationAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditLogRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleImageStorage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.CoffeeCreationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.GrantAdminUser;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ListAdminUsers;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.PreviewGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RevokeAdminUser;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RecordAdminAudit;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchGooglePlacesForCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StoreStudioArticleImage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SubmitStudioArticle;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StartStudioArticleGeneration;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminSecurityProperties;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeeGooglePlaceLookupPort;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

@Configuration
@EnableConfigurationProperties({ GooglePlacesProperties.class, ArticleImageStorageProperties.class })
public class AdminImportContextConfiguration {
	@Bean
	SearchGooglePlacesForCoffee searchGooglePlacesForCoffee(GooglePlacesGateway googlePlacesGateway) {
		return new SearchGooglePlacesForCoffee(googlePlacesGateway);
	}

	@Bean
	PreviewGooglePlaceCoffee previewGooglePlaceCoffee(GooglePlacesGateway googlePlacesGateway) {
		return new PreviewGooglePlaceCoffee(googlePlacesGateway);
	}

	@Bean
	ImportGooglePlaceCoffee importGooglePlaceCoffee(PreviewGooglePlaceCoffee previewGooglePlaceCoffee,
			CoffeeCreationPort coffeeCreationPort,
			UuidGenerator uuidGenerator,
			DateTimeProvider dateTimeProvider) {
		return new ImportGooglePlaceCoffee(previewGooglePlaceCoffee, coffeeCreationPort, uuidGenerator, dateTimeProvider);
	}

	@Bean
	CoffeeCreationPort coffeeCreationPort(CommandBus commandBus,
			CoffeeGooglePlaceLookupPort coffeeGooglePlaceLookupPort) {
		return new CommandBusCoffeeCreationPort(commandBus, coffeeGooglePlaceLookupPort);
	}

	@Bean
	ArticleAuthoringPort articleAuthoringPort(CommandBus commandBus) {
		return new CommandBusArticleAuthoringPort(commandBus);
	}

	@Bean ArticleGenerationAuthoringPort articleGenerationAuthoringPort(CommandBus commandBus) { return new CommandBusArticleGenerationAuthoringPort(commandBus); }
	@Bean StartStudioArticleGeneration startStudioArticleGeneration(ArticleGenerationAuthoringPort port, UuidGenerator ids, DateTimeProvider clock) { return new StartStudioArticleGeneration(port,ids,clock); }

	@Bean
	SubmitStudioArticle submitStudioArticle(
			ArticleAuthoringPort articleAuthoringPort,
			UuidGenerator uuidGenerator,
			DateTimeProvider dateTimeProvider,
			ObjectMapper objectMapper) {
		return new SubmitStudioArticle(articleAuthoringPort, uuidGenerator, dateTimeProvider, objectMapper);
	}

	@Bean
	StoreStudioArticleImage storeStudioArticleImage(ArticleImageStorage articleImageStorage) {
		return new StoreStudioArticleImage(articleImageStorage);
	}

	@Bean
	UuidGenerator uuidGenerator() {
		return UUID::randomUUID;
	}

	@Bean
	ListAdminUsers listAdminUsers(AdminUserAccessRepository repository, AdminSecurityProperties properties) {
		return new ListAdminUsers(repository, properties);
	}

	@Bean
	GrantAdminUser grantAdminUser(AdminUserAccessRepository repository) {
		return new GrantAdminUser(repository);
	}

	@Bean
	RevokeAdminUser revokeAdminUser(AdminUserAccessRepository repository, AdminSecurityProperties properties) {
		return new RevokeAdminUser(repository, properties);
	}

	@Bean
	RecordAdminAudit recordAdminAudit(AdminAuditLogRepository repository) { return new RecordAdminAudit(repository); }
}
