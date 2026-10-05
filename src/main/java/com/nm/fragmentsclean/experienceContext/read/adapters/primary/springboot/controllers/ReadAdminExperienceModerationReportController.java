package com.nm.fragmentsclean.experienceContext.read.adapters.primary.springboot.controllers;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nm.fragmentsclean.experienceContext.read.GetExperienceModerationReportQuery;
import com.nm.fragmentsclean.experienceContext.read.GetExperienceModerationReportQueryHandler;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceModerationReportView;
@RestController public final class ReadAdminExperienceModerationReportController {
 private final GetExperienceModerationReportQueryHandler queries;
 public ReadAdminExperienceModerationReportController(GetExperienceModerationReportQueryHandler queries) { this.queries=queries; }
 @GetMapping("/api/admin/experience-moderation/reports/{reportId}")
 public ResponseEntity<ExperienceModerationReportView> detail(@PathVariable UUID reportId) { return ResponseEntity.of(queries.handle(new GetExperienceModerationReportQuery(reportId))); }
}
