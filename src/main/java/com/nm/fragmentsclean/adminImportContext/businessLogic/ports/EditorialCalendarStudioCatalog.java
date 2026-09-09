package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface EditorialCalendarStudioCatalog {
    List<Item> month(YearMonth month);
    record Item(UUID scheduleId, UUID articleId, UUID revisionId, String operation,
                Instant dueAt, String status, String rejectionReason) { }
}
