package com.nm.fragmentsclean.socialContext.read;
import java.time.Instant;import java.util.List;import java.util.UUID;
public final class AdminCommentViews {
 private AdminCommentViews(){}
 public record Comment(UUID id,UUID targetId,UUID parentId,UUID authorId,String body,Instant createdAt,Instant editedAt,Instant deletedAt,String moderation){}
 public record Page(List<Comment> items,String nextCursor){}
}
