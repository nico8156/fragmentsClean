package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditCursor;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditPage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchAdminAuditQuery;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchAdminAuditQueryHandler;

@RestController
public final class AdminAuditReadController {
    private final SearchAdminAuditQueryHandler queries;
    public AdminAuditReadController(SearchAdminAuditQueryHandler queries) { this.queries = queries; }

    @GetMapping("/api/admin/operations/audit")
    public AdminAuditPage search(@RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId, @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) UUID commandId, @RequestParam(required = false) String action,
            @RequestParam(required = false) String outcome, @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "30") int limit) {
        return queries.handle(new SearchAdminAuditQuery(targetType, targetId, actorId, commandId,
                action, outcome, AdminAuditCursor.parse(cursor), limit));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid() { return Map.of("error", "INVALID_QUERY"); }
}
