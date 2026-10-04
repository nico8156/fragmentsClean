package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditEntry;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ListAdminAuditEntriesForTarget;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Studio ACL read endpoint for the durable operational journal of one article upload. */
@RestController
public class AdminArticleMediaOperationJournalController {
    private final ListAdminAuditEntriesForTarget listAuditEntries;

    public AdminArticleMediaOperationJournalController(ListAdminAuditEntriesForTarget listAuditEntries) {
        this.listAuditEntries = listAuditEntries;
    }

    @GetMapping("/api/admin/studio/article-media/{mediaId}/operations")
    public List<OperationResponse> listMediaOperations(@PathVariable UUID mediaId,
                                                         @RequestParam(defaultValue = "30") int limit) {
        return listAuditEntries.execute("ARTICLE_MEDIA", mediaId, limit).stream().map(OperationResponse::from).toList();
    }

    public record OperationResponse(UUID id, UUID actorUserId, String action, UUID commandId,
                                    String outcome, String reason, Instant occurredAt) {
        static OperationResponse from(AdminAuditEntry entry) {
            return new OperationResponse(entry.id(), entry.actorUserId(), entry.action(), entry.commandId(),
                    entry.outcome(), entry.reason(), entry.occurredAt());
        }
    }
}
