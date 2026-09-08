package com.nm.fragmentsclean.adminImportContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditEntry;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditLogRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ListAdminAuditEntriesForTarget;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListAdminAuditEntriesForTargetTest {
    @Test
    void delegates_a_bounded_coffee_journal_query_to_its_read_port() {
        var targetId = UUID.randomUUID();
        var repository = new RecordingRepository();
        repository.entries = List.of(new AdminAuditEntry(UUID.randomUUID(), UUID.randomUUID(), "COFFEE_PUBLISHED",
                "COFFEE", targetId, UUID.randomUUID(), "ACCEPTED", null, Instant.parse("2026-09-08T09:00:00Z")));

        var result = new ListAdminAuditEntriesForTarget(repository).execute("COFFEE", targetId, 999);

        assertThat(result).hasSize(1);
        assertThat(repository.targetType).isEqualTo("COFFEE");
        assertThat(repository.targetId).isEqualTo(targetId);
        assertThat(repository.limit).isEqualTo(100);
    }

    private static final class RecordingRepository implements AdminAuditLogRepository {
        private List<AdminAuditEntry> entries = new ArrayList<>();
        private String targetType;
        private UUID targetId;
        private int limit;
        @Override public void append(AdminAuditEntry entry) { }
        @Override public List<AdminAuditEntry> findByTarget(String targetType, UUID targetId, int limit) {
            this.targetType = targetType; this.targetId = targetId; this.limit = limit; return entries;
        }
    }
}
