package com.nm.fragmentsclean.socialContext.read;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
@Component public final class SearchAdminCommentsQueryHandler implements QueryHandler<SearchAdminCommentsQuery,AdminCommentViews.Page> {
 private final AdminCommentReadRepository repository;
 public SearchAdminCommentsQueryHandler(AdminCommentReadRepository repository){this.repository=repository;}
 public AdminCommentViews.Page handle(SearchAdminCommentsQuery query){return repository.search(query);}
}
