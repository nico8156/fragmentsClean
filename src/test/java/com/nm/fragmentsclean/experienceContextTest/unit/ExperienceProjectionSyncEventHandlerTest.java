package com.nm.fragmentsclean.experienceContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceProjectionRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceProjectionEventHandler;
import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExperienceProjectionSyncEventHandlerTest {
  @Test
  void media_change_refreshes_the_authoritative_coffee_and_user_snapshots() {
    UUID experienceId = UUID.randomUUID();
    UUID coffeeId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    var projection = new RecordingProjectionRepository();
    var publisher = new RecordingPublisher();
    var handler = new ExperienceProjectionEventHandler(projection, publisher);
    var event = new ExperienceIntegrationEvents.MediaChanged(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), experienceId, userId, coffeeId,
        "AVAILABLE", "experiences/final.jpg", "image/jpeg", 128, 80, 60, "CONFIRMED", 1,
        Instant.parse("2026-09-11T20:00:00Z"), Instant.parse("2026-09-11T19:59:00Z"));

    handler.handle(event);

    assertThat(projection.applied).isSameAs(event);
    assertThat(publisher.events)
        .extracting(ProjectionSyncEvent::projection, ProjectionSyncEvent::scope,
            ProjectionSyncEvent::entityId)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("experiences", "coffee", coffeeId.toString()),
            org.assertj.core.groups.Tuple.tuple("experiences", "user", userId.toString()),
            org.assertj.core.groups.Tuple.tuple("experience-moderation", "experience", experienceId.toString()));
  }

  private static final class RecordingPublisher implements ProjectionSyncPublisher {
    private final List<ProjectionSyncEvent> events = new ArrayList<>();
    @Override public void publish(ProjectionSyncEvent event) { events.add(event); }
  }

  private static final class RecordingProjectionRepository implements ExperienceProjectionRepository {
    private ExperienceIntegrationEvents.MediaChanged applied;
    @Override public void apply(ExperienceIntegrationEvents.MediaChanged event) { applied = event; }
    @Override public void apply(ExperienceIntegrationEvents.SnapshotChanged event) { }
    @Override public void apply(ExperienceIntegrationEvents.Reported event) { }
    @Override public void apply(ExperienceIntegrationEvents.Moderated event) { }
    @Override public void upsertProfile(UUID userId, String displayName, String avatarUrl, long version, Instant occurredAt) { }
    @Override public void upsertCoffee(UUID coffeeId, boolean active, long version, Instant occurredAt) { }
    @Override public void upsertBlock(UUID blockId, UUID blockerId, UUID blockedUserId, boolean active, long version, Instant occurredAt) { }
  }
}
