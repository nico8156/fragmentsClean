package com.nm.fragmentsclean.experienceContext.read.projections;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceProjectionRepository;
import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

public final class ExperienceProjectionEventHandler {
	private final ExperienceProjectionRepository projections;
	private final ProjectionSyncPublisher sync;

	public ExperienceProjectionEventHandler(
			ExperienceProjectionRepository projections,
			ProjectionSyncPublisher sync) {
		this.projections = projections;
		this.sync = sync;
	}

	public void handle(ExperienceIntegrationEvents.SnapshotChanged event) {
		projections.apply(event);
		publishPublic("experiences", "coffee", event.coffeeId(), event.version(), event.occurredAt(), event.reason());
		publishUser(event.userId(), "experiences", "user", event.userId(), event.version(), event.occurredAt(), event.reason());
	}

	public void handle(ExperienceIntegrationEvents.Reported event) {
		projections.apply(event);
		publishAdmin("experience-moderation", "report", event.reportId(), event.version(), event.occurredAt(), "reported");
		publishUser(event.reporterId(), "experiences", "user", event.reporterId(), event.version(), event.occurredAt(), "reported");
	}

	public void handle(ExperienceIntegrationEvents.Moderated event) {
		projections.apply(event);
		publishAdmin("experience-moderation", "report", event.reportId(), event.version(), event.occurredAt(), "moderated");
		publishPublic("experiences", "coffee", event.coffeeId(), event.version(), event.occurredAt(), "moderated");
		publishUser(event.authorId(), "experiences", "user", event.authorId(), event.version(), event.occurredAt(), "moderated");
	}

	public void handle(ExperienceIntegrationEvents.MediaChanged event) {
		projections.apply(event);
		publishPublic("experiences", "coffee", event.coffeeId(), event.version(), event.occurredAt(), "media");
		if (event.userId() != null) {
			publishUser(event.userId(), "experiences", "user", event.userId(), event.version(), event.occurredAt(), "media");
		}
		publishAdmin("experience-moderation", "experience", event.experienceId(), event.version(), event.occurredAt(), "media");
	}

	private void publishPublic(
			String projection, String scope, UUID id, long version, Instant at, String reason) {
		sync.publish(ProjectionSyncEvent.publicProjectionUpdated(
				projection, scope, id.toString(), version, at, List.of(reason)));
	}

	private void publishUser(
			UUID recipientId,
			String projection,
			String scope,
			UUID id,
			long version,
			Instant at,
			String reason) {
		sync.publish(ProjectionSyncEvent.userProjectionUpdated(
				recipientId.toString(), projection, scope, id.toString(), version, at, List.of(reason)));
	}

	private void publishAdmin(
			String projection, String scope, UUID id, long version, Instant at, String reason) {
		sync.publish(ProjectionSyncEvent.adminProjectionUpdated(
				projection, scope, id.toString(), version, at, List.of(reason)));
	}
}
