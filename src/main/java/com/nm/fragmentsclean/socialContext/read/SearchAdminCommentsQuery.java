package com.nm.fragmentsclean.socialContext.read;
import java.util.Set;import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
public record SearchAdminCommentsQuery(UUID authorId,String moderation,AdminCommentCursor cursor,int limit) implements Query<AdminCommentViews.Page> {
 public SearchAdminCommentsQuery {
  if(authorId==null||limit<1||limit>100)throw new IllegalArgumentException("Invalid author or page size");
  if(moderation!=null&&!Set.of("PUBLISHED","PENDING","REJECTED","HIDDEN","SOFT_DELETED").contains(moderation))throw new IllegalArgumentException("Invalid moderation status");
 }
}
