package com.nm.fragmentsclean.experienceContext.read.projections;

import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceProjectionRepository;
import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.*;
import java.util.List;

public final class ExperienceProjectionEventHandler{
    private final ExperienceProjectionRepository projections;private final ProjectionSyncPublisher sync;
    public ExperienceProjectionEventHandler(ExperienceProjectionRepository projections,ProjectionSyncPublisher sync){this.projections=projections;this.sync=sync;}
    public void handle(ExperienceIntegrationEvents.SnapshotChanged event){projections.apply(event);publish("experiences","coffee",event.coffeeId().toString(),event.version(),event.occurredAt(),event.reason());publish("experiences","user",event.userId().toString(),event.version(),event.occurredAt(),event.reason());}
    public void handle(ExperienceIntegrationEvents.Reported event){projections.apply(event);publish("experience-moderation","report",event.reportId().toString(),event.version(),event.occurredAt(),"reported");publish("experiences","user",event.reporterId().toString(),event.version(),event.occurredAt(),"reported");}
    public void handle(ExperienceIntegrationEvents.Moderated event){projections.apply(event);publish("experience-moderation","report",event.reportId().toString(),event.version(),event.occurredAt(),"moderated");publish("experiences","coffee",event.coffeeId().toString(),event.version(),event.occurredAt(),"moderated");publish("experiences","user",event.authorId().toString(),event.version(),event.occurredAt(),"moderated");}
    private void publish(String projection,String scope,String id,long version,java.time.Instant at,String reason){sync.publish(ProjectionSyncEvent.projectionUpdated(projection,scope,id,version,at,List.of(reason)));}
}
