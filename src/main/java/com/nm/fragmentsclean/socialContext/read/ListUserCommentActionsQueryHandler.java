package com.nm.fragmentsclean.socialContext.read;
import org.springframework.stereotype.Component;import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
@Component public final class ListUserCommentActionsQueryHandler implements QueryHandler<ListUserCommentActionsQuery,AdminCommentViews.Actions> {
 private final AdminCommentReadRepository repository;
 public ListUserCommentActionsQueryHandler(AdminCommentReadRepository repository){this.repository=repository;}
 public AdminCommentViews.Actions handle(ListUserCommentActionsQuery query){return repository.actions(query);}
}
