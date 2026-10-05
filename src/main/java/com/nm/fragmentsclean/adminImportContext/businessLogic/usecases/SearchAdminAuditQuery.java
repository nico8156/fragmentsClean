package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.util.UUID;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditCursor;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditPage;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

public record SearchAdminAuditQuery(String targetType, UUID targetId, UUID actorId,
        UUID commandId, String action, String outcome, AdminAuditCursor cursor, int limit)
        implements Query<AdminAuditPage> {
    public SearchAdminAuditQuery {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Invalid page size");
        targetType = filter(targetType, 64);
        action = filter(action, 64);
        outcome = filter(outcome, 32);
    }
    private static String filter(String value, int maximum) {
        if (value == null || value.isBlank()) return null;
        value = value.strip();
        if (value.length() > maximum) throw new IllegalArgumentException("Invalid audit filter");
        return value;
    }
}
