package com.nm.fragmentsclean.socialContext.read;
import java.util.Set;import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
public record ListUserCommentReportsQuery(UUID authorId,String status,AdminCommentCursor cursor,int limit) implements Query<AdminCommentViews.Reports> {
 public ListUserCommentReportsQuery {
  if(authorId==null||limit<1||limit>100)throw new IllegalArgumentException("Invalid author or page size");
  if(status!=null&&!Set.of("OPEN","RESOLVED","DISMISSED").contains(status))throw new IllegalArgumentException("Invalid report status");
 }
}
