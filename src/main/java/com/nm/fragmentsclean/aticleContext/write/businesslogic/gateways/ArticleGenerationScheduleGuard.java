package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways;

import java.time.Instant;

public interface ArticleGenerationScheduleGuard {
    boolean mayRequest(String subject, Instant now, int maxPending, int deduplicationHours);
}
