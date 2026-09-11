package com.nm.fragmentsclean.experienceContext.write.adapters.primary.springboot.controllers;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceReportReason;import java.time.Instant;import java.util.Objects;import java.util.UUID;
public record ReportExperienceRequest(UUID commandId,UUID reportId,ExperienceReportReason reason,String details,Instant at){public ReportExperienceRequest{Objects.requireNonNull(commandId);Objects.requireNonNull(reportId);Objects.requireNonNull(reason);Objects.requireNonNull(at);}}
