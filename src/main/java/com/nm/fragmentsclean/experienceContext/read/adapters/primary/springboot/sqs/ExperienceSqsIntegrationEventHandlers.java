package com.nm.fragmentsclean.experienceContext.read.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.EXPERIENCES_EVENTS;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceProjectionRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceProjectionEventHandler;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.EraseExperienceAccountData;
import com.nm.fragmentsclean.platform.eventing.contracts.*;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.util.function.Consumer;
import org.springframework.context.annotation.*;

@Configuration
public class ExperienceSqsIntegrationEventHandlers{
    private final SqsIntegrationEventPayloadReader reader;public ExperienceSqsIntegrationEventHandlers(SqsIntegrationEventPayloadReader reader){this.reader=reader;}
    @Bean SqsIntegrationEventHandler experienceSnapshotChanged(ExperienceProjectionEventHandler handler){return route("experience.snapshot.changed",e->handler.handle(reader.read(e,ExperienceIntegrationEvents.SnapshotChanged.class)));}
    @Bean SqsIntegrationEventHandler experienceReported(ExperienceProjectionEventHandler handler){return route("experience.reported",e->handler.handle(reader.read(e,ExperienceIntegrationEvents.Reported.class)));}
    @Bean SqsIntegrationEventHandler experienceModerated(ExperienceProjectionEventHandler handler){return route("experience.moderated",e->handler.handle(reader.read(e,ExperienceIntegrationEvents.Moderated.class)));}
    @Bean SqsIntegrationEventHandler experienceUserCreated(ExperienceProjectionRepository p){return route("app.user.created",e->{var v=reader.read(e,AppUserCreatedIntegrationEvent.class);p.upsertProfile(v.userId(),v.displayName(),v.avatarUrl(),v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceUserUpdated(ExperienceProjectionRepository p){return route("app.user.profile_updated",e->{var v=reader.read(e,AppUserProfileUpdatedIntegrationEvent.class);p.upsertProfile(v.userId(),v.displayName(),v.avatarUrl(),v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceCoffeeCreated(ExperienceProjectionRepository p){return route("coffee.created",e->{var v=reader.read(e,CoffeeCreatedIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),!"ARCHIVED".equals(v.publicationStatus()),v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceCoffeePublished(ExperienceProjectionRepository p){return route("coffee.published",e->{var v=reader.read(e,CoffeePublishedIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),true,v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceCoffeeUnpublished(ExperienceProjectionRepository p){return route("coffee.unpublished",e->{var v=reader.read(e,CoffeeLifecycleIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),false,v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceCoffeeArchived(ExperienceProjectionRepository p){return route("coffee.archived",e->{var v=reader.read(e,CoffeeLifecycleIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),false,v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceCoffeeDeleted(ExperienceProjectionRepository p){return route("coffee.deleted",e->{var v=reader.read(e,CoffeeLifecycleIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),false,v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceUserBlockChanged(ExperienceProjectionRepository p){return route("social.user_block.changed",e->{var v=reader.read(e,SocialCommentIntegrationEvents.UserBlockChanged.class);p.upsertBlock(v.blockId(),v.blockerId(),v.blockedUserId(),v.active(),v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceAccountDeletion(EraseExperienceAccountData eraser){return route("app.user.deletion_requested",e->eraser.handle(reader.read(e,AppUserDeletionRequestedIntegrationEvent.class)));}
    private SqsIntegrationEventHandler route(String type,Consumer<IntegrationEventEnvelope> consumer){return new SqsIntegrationEventHandler(){public SqsIntegrationEventRoute route(){return new SqsIntegrationEventRoute(EXPERIENCES_EVENTS,type);}public void handle(IntegrationEventEnvelope envelope){consumer.accept(envelope);}};}
}
