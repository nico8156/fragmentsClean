package com.nm.fragmentsclean.socialContext.read;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
@Component public final class ListUserCommentReportsQueryHandler implements QueryHandler<ListUserCommentReportsQuery,AdminCommentViews.Reports> {
 private final AdminCommentReadRepository repository;
 public ListUserCommentReportsQueryHandler(AdminCommentReadRepository repository){this.repository=repository;}
 public AdminCommentViews.Reports handle(ListUserCommentReportsQuery query){return repository.reports(query);}
}
