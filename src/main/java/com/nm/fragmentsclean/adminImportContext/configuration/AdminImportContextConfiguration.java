package com.nm.fragmentsclean.adminImportContext.configuration;

import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.CommandBusArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.CommandBusArticleGenerationAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.CommandBusGeneratedArticleEditingPort;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.coffee.CommandBusCoffeeCreationPort;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial.CommandBusEditorialSourceAdministrationAdapter;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial.EditorialIntelligenceStudioCatalogAdapter;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial.TopicCandidateDecisionAdapter;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial.TopicCandidateStudioCatalogAdapter;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlacesProperties;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleGenerationAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GeneratedArticleEditingPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleGenerationReviewPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticlePublicationApprovalPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditLogRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleImageStorage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.CoffeeCreationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSourceAdministrationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSourceStudioCatalog;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.TopicCandidateDecisionPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.TopicCandidateStudioCatalog;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.RetainedTopicCandidateBriefPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.GrantAdminUser;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ListAdminUsers;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.PreviewGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RevokeAdminUser;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RecordAdminAudit;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ListAdminAuditEntriesForTarget;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchGooglePlacesForCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StoreStudioArticleImage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SaveStudioArticleDraft;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SubmitStudioArticle;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StartStudioArticleGeneration;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.EditStudioGeneratedArticle;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.GetStudioArticleGenerationReview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ApproveStudioArticlePublication;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ArchiveStudioArticle;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ManageEditorialSource;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.DecideStudioTopicCandidate;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StartArticleGenerationFromTopicCandidate;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.DecideTopicCandidate;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.TopicCandidateRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ArticleBriefEvidenceRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.CreateArticleBriefFromRetainedCandidate;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial.RetainedTopicCandidateBriefAdapter;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article.CommandBusScheduledArticleOperationAdapter;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial.EditorialPlanningAdapter;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial.EditorialCalendarStudioCatalogAdapter;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledArticleOperationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialPlanningPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialCalendarStudioCatalog;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ManageEditorialPlanning;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ScheduleArticleOperation;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.CancelArticleOperation;
import com.nm.fragmentsclean.editorialIntelligenceContext.read.EditorialCalendarCatalog;
import com.nm.fragmentsclean.editorialIntelligenceContext.read.EditorialSourceCatalog;
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

	@Bean EditorialSourceAdministrationPort editorialSourceAdministrationPort(CommandBus commandBus) { return new CommandBusEditorialSourceAdministrationAdapter(commandBus); }
	@Bean EditorialSourceStudioCatalog editorialSourceStudioCatalog(EditorialSourceCatalog catalog) { return new EditorialIntelligenceStudioCatalogAdapter(catalog); }
	@Bean ManageEditorialSource manageEditorialSource(EditorialSourceAdministrationPort port, UuidGenerator ids) { return new ManageEditorialSource(port, ids); }
	@Bean TopicCandidateDecisionPort topicCandidateDecisionPort(DecideTopicCandidate decisions) { return new TopicCandidateDecisionAdapter(decisions); }
	@Bean DecideStudioTopicCandidate decideStudioTopicCandidate(TopicCandidateDecisionPort port) { return new DecideStudioTopicCandidate(port); }
	@Bean TopicCandidateStudioCatalog topicCandidateStudioCatalog(TopicCandidateRepository candidates) { return new TopicCandidateStudioCatalogAdapter(candidates); }
	@Bean CreateArticleBriefFromRetainedCandidate createArticleBriefFromRetainedCandidate(TopicCandidateRepository candidates, ArticleBriefEvidenceRepository evidence) { return new CreateArticleBriefFromRetainedCandidate(candidates, evidence); }
	@Bean RetainedTopicCandidateBriefPort retainedTopicCandidateBriefPort(CreateArticleBriefFromRetainedCandidate briefs) { return new RetainedTopicCandidateBriefAdapter(briefs); }
	@Bean StartArticleGenerationFromTopicCandidate startArticleGenerationFromTopicCandidate(RetainedTopicCandidateBriefPort briefs, StartStudioArticleGeneration articles) { return new StartArticleGenerationFromTopicCandidate(briefs, articles); }
	@Bean ScheduledArticleOperationPort scheduledArticleOperationPort(CommandBus commandBus) { return new CommandBusScheduledArticleOperationAdapter(commandBus); }
	@Bean EditorialPlanningPort editorialPlanningPort(ScheduleArticleOperation schedule, CancelArticleOperation cancel) { return new EditorialPlanningAdapter(schedule, cancel); }
	@Bean EditorialCalendarStudioCatalog editorialCalendarStudioCatalog(EditorialCalendarCatalog catalog) { return new EditorialCalendarStudioCatalogAdapter(catalog); }
	@Bean ManageEditorialPlanning manageEditorialPlanning(EditorialPlanningPort planning, UuidGenerator ids) { return new ManageEditorialPlanning(planning, ids); }

	@Bean
	ArticleAuthoringPort articleAuthoringPort(CommandBus commandBus) {
		return new CommandBusArticleAuthoringPort(commandBus);
	}

	@Bean ArticleGenerationAuthoringPort articleGenerationAuthoringPort(CommandBus commandBus) { return new CommandBusArticleGenerationAuthoringPort(commandBus); }
	@Bean StartStudioArticleGeneration startStudioArticleGeneration(ArticleGenerationAuthoringPort port, UuidGenerator ids, DateTimeProvider clock) { return new StartStudioArticleGeneration(port,ids,clock); }
	@Bean GeneratedArticleEditingPort generatedArticleEditingPort(CommandBus commandBus){return new CommandBusGeneratedArticleEditingPort(commandBus);}
	@Bean EditStudioGeneratedArticle editStudioGeneratedArticle(GeneratedArticleEditingPort port,UuidGenerator ids,DateTimeProvider clock){return new EditStudioGeneratedArticle(port,ids,clock);}
	@Bean GetStudioArticleGenerationReview getStudioArticleGenerationReview(ArticleGenerationReviewPort port){return new GetStudioArticleGenerationReview(port);}
	@Bean ApproveStudioArticlePublication approveStudioArticlePublication(ArticlePublicationApprovalPort port){return new ApproveStudioArticlePublication(port);}

	@Bean
	SubmitStudioArticle submitStudioArticle(
			ArticleAuthoringPort articleAuthoringPort,
			UuidGenerator uuidGenerator,
			DateTimeProvider dateTimeProvider) {
		return new SubmitStudioArticle(articleAuthoringPort, uuidGenerator, dateTimeProvider);
	}

	@Bean
	SaveStudioArticleDraft saveStudioArticleDraft(
			ArticleAuthoringPort articleAuthoringPort,
			UuidGenerator uuidGenerator,
			DateTimeProvider dateTimeProvider) {
		return new SaveStudioArticleDraft(articleAuthoringPort, uuidGenerator, dateTimeProvider);
	}

	@Bean ArchiveStudioArticle archiveStudioArticle(
			ArticleAuthoringPort port, UuidGenerator ids, DateTimeProvider clock) {
		return new ArchiveStudioArticle(port, ids, clock);
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

	@Bean
	ListAdminAuditEntriesForTarget listAdminAuditEntriesForTarget(AdminAuditLogRepository repository) {
		return new ListAdminAuditEntriesForTarget(repository);
	}
}
