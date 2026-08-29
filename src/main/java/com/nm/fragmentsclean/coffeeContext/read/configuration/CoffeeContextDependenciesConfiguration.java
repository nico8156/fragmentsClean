package com.nm.fragmentsclean.coffeeContext.read.configuration;

import com.nm.fragmentsclean.coffeeContext.businessLogic.processManagers.CoffeeCreatedProcessManager;
import com.nm.fragmentsclean.coffeeContext.businessLogic.processManagers.CoffeeCreatedIntegrationEnrichmentHandler;
import com.nm.fragmentsclean.coffeeContext.businessLogic.processManagers.CoffeeDeletedMediaCleanupHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeArchivedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedIntegrationEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeDeletedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeOpeningHoursImportedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoAddedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoDeletedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoUriResolver;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotosImportedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePublishedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.ListCoffeesQueryHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.storage.DefaultCoffeePhotoUriResolver;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionSource;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlacePhotosGateway;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlaceOpeningHoursGateway;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.JpaCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.SpringCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.CoffeePhotoStorageProperties;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ArchiveCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.DeleteCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.AddCoffeePhotoCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.DeleteCoffeePhotoCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.PublishCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ImportGoogleOpeningHoursForCoffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ImportGooglePhotosForCoffee;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

import java.util.List;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(CoffeePhotoStorageProperties.class)
@EntityScan("com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities")
@EnableJpaRepositories("com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa")
@ComponentScan(basePackages = {
		"com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot"
})
public class CoffeeContextDependenciesConfiguration {

	@Bean
	public JpaCoffeeRepository jpaCoffeeRepository(SpringCoffeeRepository springCoffeeRepository) {
		return new JpaCoffeeRepository(springCoffeeRepository);
	}

	@Bean
	CreateCoffeeCommandHandler createCoffeeCommandHandler(CoffeeRepository coffeeRepository,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		return new CreateCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
	}

	@Bean
	ArchiveCoffeeCommandHandler archiveCoffeeCommandHandler(CoffeeRepository coffeeRepository,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		return new ArchiveCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
	}

	@Bean
	PublishCoffeeCommandHandler publishCoffeeCommandHandler(CoffeeRepository coffeeRepository,
			DomainEventPublisher domainEventPublisher, DateTimeProvider dateTimeProvider) {
		return new PublishCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
	}

	@Bean
	DeleteCoffeeCommandHandler deleteCoffeeCommandHandler(CoffeeRepository coffeeRepository,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		return new DeleteCoffeeCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
	}

	@Bean
	AddCoffeePhotoCommandHandler addCoffeePhotoCommandHandler(CoffeeRepository coffeeRepository,
			CoffeePhotoStorage coffeePhotoStorage,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		return new AddCoffeePhotoCommandHandler(
				coffeeRepository,
				coffeePhotoStorage,
				domainEventPublisher,
				dateTimeProvider);
	}

	@Bean
	DeleteCoffeePhotoCommandHandler deleteCoffeePhotoCommandHandler(CoffeeRepository coffeeRepository,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		return new DeleteCoffeePhotoCommandHandler(coffeeRepository, domainEventPublisher, dateTimeProvider);
	}

	@Bean
	ListCoffeesQueryHandler listCoffeesQueryHandler(CoffeeProjectionRepository coffeeProjectionRepository) {
		return new ListCoffeesQueryHandler(coffeeProjectionRepository);
	}

	@Bean
	CoffeeCreatedEventHandler coffeeCreatedEventHandler(
			CoffeeProjectionRepository projectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new CoffeeCreatedEventHandler(projectionRepository, projectionSyncPublisher);
	}

	@Bean
	CoffeePublishedEventHandler coffeePublishedEventHandler(CoffeeProjectionRepository repository,
			ProjectionSyncPublisher syncPublisher) {
		return new CoffeePublishedEventHandler(repository, syncPublisher);
	}

	@Bean
	CoffeeCreatedIntegrationEventHandler coffeeCreatedIntegrationEventHandler(
			CoffeeProjectionSource projectionSource,
			CoffeeProjectionRepository projectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new CoffeeCreatedIntegrationEventHandler(projectionSource, projectionRepository, projectionSyncPublisher);
	}

	@Bean
	CoffeeCreatedProcessManager coffeeCreatedProcessManager(
			CoffeeCreatedEventHandler coffeeCreatedEventHandler,
			ImportGoogleOpeningHoursForCoffee importGoogleOpeningHoursForCoffee,
			ImportGooglePhotosForCoffee importGooglePhotosForCoffee) {
		return new CoffeeCreatedProcessManager(List.of(
				coffeeCreatedEventHandler,
				importGoogleOpeningHoursForCoffee,
				importGooglePhotosForCoffee));
	}

	@Bean
	CoffeeCreatedIntegrationEnrichmentHandler coffeeCreatedIntegrationEnrichmentHandler(
			ImportGoogleOpeningHoursForCoffee openingHoursImporter,
			ImportGooglePhotosForCoffee photosImporter) {
		return new CoffeeCreatedIntegrationEnrichmentHandler(openingHoursImporter, photosImporter);
	}

	@Bean
	CoffeeDeletedMediaCleanupHandler coffeeDeletedMediaCleanupHandler(CoffeePhotoStorage photoStorage) {
		return new CoffeeDeletedMediaCleanupHandler(photoStorage);
	}

	@Bean
	CoffeeArchivedEventHandler coffeeArchivedEventHandler(
			CoffeeProjectionRepository projectionRepository,
			CoffeePhotoProjectionRepository photoProjectionRepository,
			CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new CoffeeArchivedEventHandler(
				projectionRepository,
				photoProjectionRepository,
				openingHoursProjectionRepository,
				projectionSyncPublisher);
	}

	@Bean
	CoffeeDeletedEventHandler coffeeDeletedEventHandler(
			CoffeeProjectionRepository projectionRepository,
			CoffeePhotoProjectionRepository photoProjectionRepository,
			CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new CoffeeDeletedEventHandler(
				projectionRepository,
				photoProjectionRepository,
				openingHoursProjectionRepository,
				projectionSyncPublisher);
	}

	@Bean
	ImportGoogleOpeningHoursForCoffee importGoogleOpeningHoursForCoffee(
			GooglePlaceOpeningHoursGateway openingHoursGateway,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		return new ImportGoogleOpeningHoursForCoffee(openingHoursGateway, domainEventPublisher, dateTimeProvider);
	}

	@Bean
	CoffeeOpeningHoursImportedEventHandler coffeeOpeningHoursImportedEventHandler(
			CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository,
			CoffeeProjectionRepository coffeeProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new CoffeeOpeningHoursImportedEventHandler(
				openingHoursProjectionRepository, coffeeProjectionRepository, projectionSyncPublisher);
	}

	@Bean
	ImportGooglePhotosForCoffee importGooglePhotosForCoffee(
			GooglePlacePhotosGateway photosGateway,
			CoffeePhotoStorage photoStorage,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		return new ImportGooglePhotosForCoffee(photosGateway, photoStorage, domainEventPublisher, dateTimeProvider);
	}

	@Bean
	CoffeePhotosImportedEventHandler coffeePhotosImportedEventHandler(
			CoffeePhotoProjectionRepository photoProjectionRepository,
			CoffeeProjectionRepository coffeeProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new CoffeePhotosImportedEventHandler(
				photoProjectionRepository, coffeeProjectionRepository, projectionSyncPublisher);
	}

	@Bean
	CoffeePhotoAddedEventHandler coffeePhotoAddedEventHandler(
			CoffeePhotoProjectionRepository photoProjectionRepository,
			CoffeeProjectionRepository coffeeProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new CoffeePhotoAddedEventHandler(
				photoProjectionRepository, coffeeProjectionRepository, projectionSyncPublisher);
	}

	@Bean
	CoffeePhotoDeletedEventHandler coffeePhotoDeletedEventHandler(
			CoffeePhotoProjectionRepository photoProjectionRepository,
			CoffeeProjectionRepository coffeeProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		return new CoffeePhotoDeletedEventHandler(
				photoProjectionRepository, coffeeProjectionRepository, projectionSyncPublisher);
	}

	@Bean
	CoffeePhotoUriResolver coffeePhotoUriResolver(
			CoffeePhotoStorageProperties properties,
			@Qualifier("coffeePhotoS3Presigner") ObjectProvider<S3Presigner> s3Presigner) {
		return new DefaultCoffeePhotoUriResolver(properties, s3Presigner.getIfAvailable());
	}
}
