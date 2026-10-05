package com.nm.fragmentsclean.socialContext.read;
import java.util.UUID;import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
public record ListUserCommentActionsQuery(UUID authorId,AdminCommentCursor cursor,int limit) implements Query<AdminCommentViews.Actions> {
 public ListUserCommentActionsQuery {if(authorId==null||limit<1||limit>100)throw new IllegalArgumentException("Invalid author or page size");}
}
