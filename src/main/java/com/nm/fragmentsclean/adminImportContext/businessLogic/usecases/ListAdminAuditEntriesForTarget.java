package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditEntry;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditLogRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Read-side audit query owned by the Studio/admin boundary, never by a product aggregate. */
public class ListAdminAuditEntriesForTarget {
    private static final int MAX_LIMIT = 100;
    private final AdminAuditLogRepository repository;

    public ListAdminAuditEntriesForTarget(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    public List<AdminAuditEntry> execute(String targetType, UUID targetId, int requestedLimit) {
        if (targetType == null || targetType.isBlank()) throw new IllegalArgumentException("target type is required");
        Objects.requireNonNull(targetId, "target id is required");
        int limit = requestedLimit <= 0 ? 30 : Math.min(requestedLimit, MAX_LIMIT);
        return repository.findByTarget(targetType, targetId, limit);
    }
}
