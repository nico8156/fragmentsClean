package com.nm.fragmentsclean.mediaCatalogContext.read.adapters.primary;
import com.nm.fragmentsclean.mediaCatalogContext.read.*;
import com.nm.fragmentsclean.platform.eventing.contracts.*;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.*;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.context.annotation.*;
import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.MEDIA_CATALOG_EVENTS;
@Configuration
public class MediaCatalogEventsConfiguration {
    @Bean ArticleMediaCatalogHandler articleMediaCatalogHandler(ArticleMediaCatalogProjection projection){return new ArticleMediaCatalogHandler(projection);}
    @Bean SqsIntegrationEventHandler catalogArticleMedia(SqsIntegrationEventPayloadReader reader,ArticleMediaCatalogHandler handler,ProjectionSyncPublisher sync){
        return route("article.media_catalog_snapshot",envelope->{
            if(envelope.eventVersion()!=1)throw new IllegalArgumentException("Unsupported article media snapshot version");
            var event=reader.read(envelope,ArticleMediaCatalogSnapshotIntegrationEvent.class);
            if(handler.handle(event))sync.publish(ProjectionSyncEvent.adminProjectionUpdated("media-catalog","article",event.articleId().toString(),event.version(),event.occurredAt(),List.of("media")));
        });
    }
    @Bean MediaCatalogProjectionHandler mediaCatalogProjectionHandler(MediaCatalogProjection projection){return new MediaCatalogProjectionHandler(projection);}
    @Bean SqsIntegrationEventHandler catalogExperienceMedia(SqsIntegrationEventPayloadReader reader,
        MediaCatalogProjectionHandler handler, AccountErasureBarrier barrier, ProjectionSyncPublisher sync) {
        return route("experience.media.changed", envelope->{
            var event=reader.read(envelope,ExperienceIntegrationEvents.MediaChanged.class);
            Runnable apply=()->{
                handler.handle(event);
                sync.publish(ProjectionSyncEvent.adminProjectionUpdated("media-catalog","media",event.mediaId().toString(),event.version(),event.occurredAt(),List.of("media")));
            };
            if("DELETED".equals(event.status())) apply.run();
            else if(event.userId()!=null) barrier.ifActive(AccountErasureBarrier.Scope.MEDIA_CATALOG,event.userId(),apply);
        });
    }
    @Bean SqsIntegrationEventHandler catalogAvatarMedia(SqsIntegrationEventPayloadReader reader,
        MediaCatalogProjectionHandler handler, AccountErasureBarrier barrier, ProjectionSyncPublisher sync) {
        return route("avatar.media.changed", envelope->{
            var event=reader.read(envelope,AvatarMediaChangedIntegrationEvent.class);
            Runnable apply=()->{
                handler.handle(event);
                sync.publish(ProjectionSyncEvent.adminProjectionUpdated("media-catalog","media",event.mediaId().toString(),event.version(),event.occurredAt(),List.of("media")));
            };
            if("DELETED".equals(event.status())) apply.run();
            else if(event.userId()!=null) barrier.ifActive(AccountErasureBarrier.Scope.MEDIA_CATALOG,event.userId(),apply);
        });
    }
    @Bean CoffeeMediaCatalogHandler coffeeMediaCatalogHandler(CoffeeMediaCatalogProjection projection){return new CoffeeMediaCatalogHandler(projection);}
    @Bean SqsIntegrationEventHandler catalogCoffeePhotoAdded(SqsIntegrationEventPayloadReader reader,CoffeeMediaCatalogHandler handler,ProjectionSyncPublisher sync){return coffeeRoute("coffee.photo_added",CoffeePhotoAddedIntegrationEvent.class,reader,handler::handle,sync);}
    @Bean SqsIntegrationEventHandler catalogCoffeePhotoDeleted(SqsIntegrationEventPayloadReader reader,CoffeeMediaCatalogHandler handler,ProjectionSyncPublisher sync){return coffeeRoute("coffee.photo_deleted",CoffeePhotoDeletedIntegrationEvent.class,reader,handler::handle,sync);}
    @Bean SqsIntegrationEventHandler catalogCoffeePhotosImported(SqsIntegrationEventPayloadReader reader,CoffeeMediaCatalogHandler handler,ProjectionSyncPublisher sync){return coffeeRoute("coffee.photos_imported",CoffeePhotosImportedIntegrationEvent.class,reader,handler::handle,sync);}
    @Bean SqsIntegrationEventHandler catalogCoffeePhotosArranged(SqsIntegrationEventPayloadReader reader,CoffeeMediaCatalogHandler handler,ProjectionSyncPublisher sync){return coffeeRoute("coffee.photos_arranged",CoffeePhotosArrangedIntegrationEvent.class,reader,handler::handle,sync);}
    @Bean SqsIntegrationEventHandler catalogCoffeeSnapshot(SqsIntegrationEventPayloadReader reader,CoffeeMediaCatalogHandler handler,ProjectionSyncPublisher sync){
        return route("coffee.media_catalog_snapshot",envelope->{
            if(envelope.eventVersion()==1)handler.handle(reader.read(envelope,CoffeePhotosArrangedIntegrationEvent.class));
            else if(envelope.eventVersion()==2)handler.handle(reader.read(envelope,CoffeeMediaCatalogSnapshotIntegrationEvent.class));
            else if(envelope.eventVersion()==3)handler.handle(reader.read(envelope,CoffeeMediaCatalogSnapshotV3IntegrationEvent.class));
            else throw new IllegalArgumentException("Unsupported coffee media snapshot version");
            sync.publish(ProjectionSyncEvent.adminProjectionUpdated("media-catalog","media",envelope.aggregateId(),null,envelope.occurredAt(),List.of("media")));
        });
    }
    @Bean SqsIntegrationEventHandler catalogCoffeeDeleted(SqsIntegrationEventPayloadReader reader,CoffeeMediaCatalogHandler handler,ProjectionSyncPublisher sync){return coffeeRoute("coffee.deleted",CoffeeLifecycleIntegrationEvent.class,reader,handler::deleted,sync);}
    private <T> SqsIntegrationEventHandler coffeeRoute(String type,Class<T> contract,SqsIntegrationEventPayloadReader reader,Consumer<T> apply,ProjectionSyncPublisher sync){
        return route(type,envelope->{
            apply.accept(reader.read(envelope,contract));
            sync.publish(ProjectionSyncEvent.adminProjectionUpdated("media-catalog","media",envelope.aggregateId(),null,envelope.occurredAt(),List.of("media")));
        });
    }
    @Bean SqsIntegrationEventHandler catalogAccountErasure(SqsIntegrationEventPayloadReader reader,
        MediaCatalogProjection projection, AccountErasureBarrier barrier, DomainEventPublisher events) {
        return route("app.user.deletion_requested", envelope->{
            var request=reader.read(envelope,AppUserDeletionRequestedIntegrationEvent.class);
            barrier.erase(AccountErasureBarrier.Scope.MEDIA_CATALOG,request.userId(),request.requestId(),request.occurredAt(),()->{
                projection.erase(request.userId());
                events.publish(new MediaCatalogAccountDataErasedEvent(UUID.randomUUID(),request.requestId(),request.userId(),"MEDIA_CATALOG",request.occurredAt()));
            });
        });
    }
    private SqsIntegrationEventHandler route(String type,Consumer<IntegrationEventEnvelope> consumer){
        return new SqsIntegrationEventHandler(){
            public SqsIntegrationEventRoute route(){return new SqsIntegrationEventRoute(MEDIA_CATALOG_EVENTS,type);}
            public void handle(IntegrationEventEnvelope envelope){consumer.accept(envelope);}
        };
    }
}
