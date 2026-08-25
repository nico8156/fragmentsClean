package com.nm.fragmentsclean.adminImportContext.configuration;

import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.CommandBusArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.coffee.CommandBusCoffeeCreationPort;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlacesProperties;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleImageStorage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.CoffeeCreationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.GrantAdminUser;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ListAdminUsers;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.PreviewGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RevokeAdminUser;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchGooglePlacesForCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StoreStudioArticleImage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SubmitStudioArticle;
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
	ListAdminUsers listAdminUsers(AdminUserAccessRepository repository) {
		return new ListAdminUsers(repository);
	}

	@Bean
	GrantAdminUser grantAdminUser(AdminUserAccessRepository repository) {
		return new GrantAdminUser(repository);
	}

	@Bean
	RevokeAdminUser revokeAdminUser(AdminUserAccessRepository repository) {
		return new RevokeAdminUser(repository);
	}
}
