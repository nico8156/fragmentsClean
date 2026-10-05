package com.nm.fragmentsclean.experienceContext.read;
import java.time.Instant;import java.util.*;
import com.nm.fragmentsclean.experienceContext.read.projections.*;
public final class AdminExperienceViews {
 private AdminExperienceViews(){}
 public record Media(UUID mediaId,UUID experienceId,UUID ownerId,String status,String url,String contentType,long size,Integer width,Integer height,Instant updatedAt){}
 public record Detail(ExperienceView experience,List<Media> media,List<ExperienceModerationActionView> actions,String nextActionsCursor){}
 public record UserReport(UUID reportId,UUID experienceId,String reason,String details,String status,Instant createdAt){}
 public record UserReports(List<UserReport> items,String nextCursor){}
 public record UserAction(UUID actionId,UUID experienceId,UUID operatorId,String decision,String reason,Instant occurredAt){}
 public record UserActions(List<UserAction> items,String nextCursor){}
 public record Actions(List<ExperienceModerationActionView> items,String nextCursor){}
}
