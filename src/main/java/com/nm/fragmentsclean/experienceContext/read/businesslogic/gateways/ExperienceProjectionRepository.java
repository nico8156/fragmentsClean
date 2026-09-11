package com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways;

import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
import java.time.Instant;
import java.util.UUID;

public interface ExperienceProjectionRepository {
  void apply(ExperienceIntegrationEvents.SnapshotChanged event);

  void apply(ExperienceIntegrationEvents.Reported event);

  void apply(ExperienceIntegrationEvents.Moderated event);

  void apply(ExperienceIntegrationEvents.MediaChanged event);

  void upsertProfile(UUID userId, String displayName, String avatarUrl, long version, Instant occurredAt);

  void upsertCoffee(UUID coffeeId, boolean active, long version, Instant occurredAt);

  void upsertBlock(
      UUID blockId,
      UUID blockerId,
      UUID blockedUserId,
      boolean active,
      long version,
      Instant occurredAt);
}
