package com.nm.fragmentsclean.socialContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record ModerationActionView(UUID actionId, UUID operatorId, String decision, String reason,
                                   Instant occurredAt) { }
