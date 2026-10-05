package com.nm.fragmentsclean.socialContext.read;
import java.time.Instant;import java.util.List;import java.util.UUID;
public final class AdminCommentViews {
 private AdminCommentViews(){}
 public record Comment(UUID id,UUID targetId,UUID parentId,UUID authorId,String body,Instant createdAt,Instant editedAt,Instant deletedAt,String moderation){}
 public record Report(UUID reportId,UUID commentId,UUID targetId,String reason,String details,String status,Instant createdAt){}
 public record Reports(List<Report> items,String nextCursor){}
 public record Action(UUID actionId,UUID reportId,UUID commentId,UUID operatorId,String decision,String reason,Instant occurredAt){}
 public record Actions(List<Action> items,String nextCursor){}
 public record Page(List<Comment> items,String nextCursor){}
}
