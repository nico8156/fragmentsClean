package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialCalendarStudioCatalog;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ManageEditorialPlanning;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/editorial/calendar")
public final class AdminEditorialCalendarController {
    private final EditorialCalendarStudioCatalog calendar;
    private final ManageEditorialPlanning planning;
    public AdminEditorialCalendarController(EditorialCalendarStudioCatalog calendar, ManageEditorialPlanning planning) {
        this.calendar = calendar; this.planning = planning;
    }

    @GetMapping
    public List<EditorialCalendarStudioCatalog.Item> month(@RequestParam YearMonth month) {
        return calendar.month(month);
    }

    @PostMapping
    public ResponseEntity<ScheduledResponse> schedule(@RequestBody ScheduleRequest request) {
        UUID id = planning.schedule(request.articleId(), request.revisionId(), request.operation(), request.dueAt());
        return ResponseEntity.accepted().body(new ScheduledResponse(id));
    }

    @PostMapping("/{scheduleId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID scheduleId) {
        planning.cancel(scheduleId);
        return ResponseEntity.accepted().build();
    }

    public record ScheduleRequest(UUID articleId, UUID revisionId, String operation, Instant dueAt) { }
    public record ScheduledResponse(UUID scheduleId) { }
}
