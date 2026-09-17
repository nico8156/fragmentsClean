package com.nm.fragmentsclean.experienceContext.read.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.EXPERIENCES_EVENTS;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceProjectionRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceProjectionEventHandler;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.EraseExperienceAccountData;
import com.nm.fragmentsclean.platform.eventing.contracts.*;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.util.function.Consumer;
import java.util.List;
import org.springframework.context.annotation.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;

@Configuration
public class ExperienceSqsIntegrationEventHandlers{
    private final SqsIntegrationEventPayloadReader reader;public ExperienceSqsIntegrationEventHandlers(SqsIntegrationEventPayloadReader reader){this.reader=reader;}
    @Bean SqsIntegrationEventHandler experienceSnapshotChanged(ExperienceProjectionEventHandler handler,AccountErasureBarrier b){return route("experience.snapshot.changed",e->{var v=reader.read(e,ExperienceIntegrationEvents.SnapshotChanged.class);b.ifActive(AccountErasureBarrier.Scope.EXPERIENCE,v.userId(),()->handler.handle(v));});}
    @Bean SqsIntegrationEventHandler experienceReported(ExperienceProjectionEventHandler handler,AccountErasureBarrier b){return route("experience.reported",e->{var v=reader.read(e,ExperienceIntegrationEvents.Reported.class);b.ifAllActive(AccountErasureBarrier.Scope.EXPERIENCE,List.of(v.authorId(),v.reporterId()),()->handler.handle(v));});}
    @Bean SqsIntegrationEventHandler experienceModerated(ExperienceProjectionEventHandler handler,AccountErasureBarrier b){return route("experience.moderated",e->{var v=reader.read(e,ExperienceIntegrationEvents.Moderated.class);b.ifAllActive(AccountErasureBarrier.Scope.EXPERIENCE,List.of(v.authorId(),v.operatorId()),()->handler.handle(v));});}
    @Bean SqsIntegrationEventHandler experienceMediaChanged(ExperienceProjectionEventHandler handler,AccountErasureBarrier b){return route("experience.media.changed",e->{var v=reader.read(e,ExperienceIntegrationEvents.MediaChanged.class);b.ifActive(AccountErasureBarrier.Scope.EXPERIENCE,v.userId(),()->handler.handle(v));});}
    @Bean SqsIntegrationEventHandler experienceUserCreated(ExperienceProjectionRepository p,AccountErasureBarrier b){return route("app.user.created",e->{var v=reader.read(e,AppUserCreatedIntegrationEvent.class);b.ifActive(AccountErasureBarrier.Scope.EXPERIENCE,v.userId(),()->p.upsertProfile(v.userId(),v.displayName(),v.avatarUrl(),v.version(),v.occurredAt()));});}
    @Bean SqsIntegrationEventHandler experienceUserUpdated(ExperienceProjectionRepository p,AccountErasureBarrier b){return route("app.user.profile_updated",e->{var v=reader.read(e,AppUserProfileUpdatedIntegrationEvent.class);b.ifActive(AccountErasureBarrier.Scope.EXPERIENCE,v.userId(),()->p.upsertProfile(v.userId(),v.displayName(),v.avatarUrl(),v.version(),v.occurredAt()));});}
    @Bean SqsIntegrationEventHandler experienceCoffeeCreated(ExperienceProjectionRepository p){return route("coffee.created",e->{var v=reader.read(e,CoffeeCreatedIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),!"ARCHIVED".equals(v.publicationStatus()),v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceCoffeePublished(ExperienceProjectionRepository p){return route("coffee.published",e->{var v=reader.read(e,CoffeePublishedIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),true,v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceCoffeeUnpublished(ExperienceProjectionRepository p){return route("coffee.unpublished",e->{var v=reader.read(e,CoffeeLifecycleIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),false,v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceCoffeeArchived(ExperienceProjectionRepository p){return route("coffee.archived",e->{var v=reader.read(e,CoffeeLifecycleIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),false,v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceCoffeeDeleted(ExperienceProjectionRepository p){return route("coffee.deleted",e->{var v=reader.read(e,CoffeeLifecycleIntegrationEvent.class);p.upsertCoffee(v.coffeeId(),false,v.version(),v.occurredAt());});}
    @Bean SqsIntegrationEventHandler experienceUserBlockChanged(ExperienceProjectionRepository p,AccountErasureBarrier b){return route("social.user_block.changed",e->{var v=reader.read(e,SocialCommentIntegrationEvents.UserBlockChanged.class);b.ifAllActive(AccountErasureBarrier.Scope.EXPERIENCE,List.of(v.blockerId(),v.blockedUserId()),()->p.upsertBlock(v.blockId(),v.blockerId(),v.blockedUserId(),v.active(),v.version(),v.occurredAt()));});}
    @Bean SqsIntegrationEventHandler experienceAccountDeletion(EraseExperienceAccountData eraser){return route("app.user.deletion_requested",e->eraser.handle(reader.read(e,AppUserDeletionRequestedIntegrationEvent.class)));}
    private SqsIntegrationEventHandler route(String type,Consumer<IntegrationEventEnvelope> consumer){return new SqsIntegrationEventHandler(){public SqsIntegrationEventRoute route(){return new SqsIntegrationEventRoute(EXPERIENCES_EVENTS,type);}public void handle(IntegrationEventEnvelope envelope){consumer.accept(envelope);}};}
}
