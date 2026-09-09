package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.time.Instant;
import java.util.UUID;

public interface EditorialPlanningPort {
    UUID schedule(UUID scheduleId, UUID articleId, UUID revisionId, String operation, Instant dueAt);
    void cancel(UUID scheduleId);
}
