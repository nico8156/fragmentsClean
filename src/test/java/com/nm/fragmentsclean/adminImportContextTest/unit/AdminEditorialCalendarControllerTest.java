package com.nm.fragmentsclean.adminImportContextTest.unit;

import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.AdminEditorialCalendarController;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialCalendarStudioCatalog;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialPlanningPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ManageEditorialPlanning;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminEditorialCalendarControllerTest {
    @Test void exposes_month_schedule_and_cancellation_through_admin_acl() {
        UUID scheduleId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        Instant dueAt = Instant.parse("2026-10-12T08:00:00Z");
        var planningPort = new FakePlanning();
        EditorialCalendarStudioCatalog catalog = month -> List.of(new EditorialCalendarStudioCatalog.Item(
                scheduleId, articleId, revisionId, "PUBLISH", dueAt, "SCHEDULED", null));
        var controller = new AdminEditorialCalendarController(catalog,
                new ManageEditorialPlanning(planningPort, () -> scheduleId));

        var response = controller.schedule(new AdminEditorialCalendarController.ScheduleRequest(
                articleId, revisionId, "publish", dueAt));
        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody().scheduleId()).isEqualTo(scheduleId);
        assertThat(planningPort.operation).isEqualTo("PUBLISH");
        assertThat(controller.month(YearMonth.of(2026, 10))).hasSize(1);

        controller.cancel(scheduleId);
        assertThat(planningPort.cancelled).isEqualTo(scheduleId);
    }

    @Test void rejects_a_publication_without_an_exact_revision() {
        var planning = new ManageEditorialPlanning(new FakePlanning(), UUID::randomUUID);
        assertThatThrownBy(() -> planning.schedule(UUID.randomUUID(), null, "PUBLISH",
                Instant.parse("2026-10-12T08:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("revisionId");
    }

    private static final class FakePlanning implements EditorialPlanningPort {
        private String operation;
        private UUID cancelled;
        public UUID schedule(UUID id, UUID article, UUID revision, String operation, Instant dueAt) {
            this.operation = operation; return id;
        }
        public void cancel(UUID scheduleId) { cancelled = scheduleId; }
    }
}
