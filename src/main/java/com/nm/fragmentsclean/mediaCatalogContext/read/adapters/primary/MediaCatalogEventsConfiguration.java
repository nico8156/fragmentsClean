package com.nm.fragmentsclean.mediaCatalogContext.read.adapters.primary;
import com.nm.fragmentsclean.mediaCatalogContext.read.*;
import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
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
