package com.nm.fragmentsclean.socialContext.read;
public interface AdminCommentReadRepository {
 AdminCommentViews.Reports reports(ListUserCommentReportsQuery query);
 AdminCommentViews.Actions actions(ListUserCommentActionsQuery query);
 AdminCommentViews.Page search(SearchAdminCommentsQuery query);
}
