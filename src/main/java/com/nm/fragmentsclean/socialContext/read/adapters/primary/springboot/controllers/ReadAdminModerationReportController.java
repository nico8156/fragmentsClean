package com.nm.fragmentsclean.socialContext.read.adapters.primary.springboot.controllers;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nm.fragmentsclean.socialContext.read.GetModerationReportQuery;
import com.nm.fragmentsclean.socialContext.read.GetModerationReportQueryHandler;
import com.nm.fragmentsclean.socialContext.read.projections.ModerationReportView;
@RestController public final class ReadAdminModerationReportController {
 private final GetModerationReportQueryHandler queries;
 public ReadAdminModerationReportController(GetModerationReportQueryHandler queries) { this.queries=queries; }
 @GetMapping("/api/admin/moderation/reports/{reportId}")
 public ResponseEntity<ModerationReportView> detail(@PathVariable UUID reportId) { return ResponseEntity.of(queries.handle(new GetModerationReportQuery(reportId))); }
}
