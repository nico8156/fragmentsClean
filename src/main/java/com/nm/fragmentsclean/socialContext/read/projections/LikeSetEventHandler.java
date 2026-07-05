package com.nm.fragmentsclean.socialContext.read.projections;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcLikeProjectionRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LikeSetEventHandler implements EventHandler<LikeSetEvent> {
	private static final Logger log = LoggerFactory.getLogger(LikeSetEventHandler.class);

	private final JdbcLikeProjectionRepository projectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;

	public LikeSetEventHandler(
			JdbcLikeProjectionRepository projectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
	}

	@Override
	@Transactional
	public void handle(LikeSetEvent event) {
		log.info("[social-read] apply LikeSetEvent likeId={} targetId={} active={} v={}",
				event.likeId(), event.targetId(), event.active(), event.version());
		projectionRepository.apply(event);
		projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
				"likes",
				"target",
				event.targetId().toString(),
				event.version(),
				event.occurredAt(),
				List.of("set")));
	}
}
