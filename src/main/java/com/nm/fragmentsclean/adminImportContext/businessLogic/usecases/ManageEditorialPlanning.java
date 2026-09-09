package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialPlanningPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

public final class ManageEditorialPlanning {
    private final EditorialPlanningPort planning;
    private final UuidGenerator ids;
    public ManageEditorialPlanning(EditorialPlanningPort planning, UuidGenerator ids) { this.planning = planning; this.ids = ids; }
    public UUID schedule(UUID articleId, UUID revisionId, String operation, Instant dueAt) {
        if (articleId == null) throw new IllegalArgumentException("articleId is required");
        if (operation == null || operation.isBlank()) throw new IllegalArgumentException("operation is required");
        if (dueAt == null) throw new IllegalArgumentException("dueAt is required");
        String normalized = operation.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalized.equals("PUBLISH") && !normalized.equals("ARCHIVE")) {
            throw new IllegalArgumentException("operation must be PUBLISH or ARCHIVE");
        }
        if (normalized.equals("PUBLISH") && revisionId == null) {
            throw new IllegalArgumentException("revisionId is required for publication");
        }
        return planning.schedule(ids.generate(), articleId, revisionId, normalized, dueAt);
    }
    public void cancel(UUID scheduleId) { planning.cancel(scheduleId); }
}
