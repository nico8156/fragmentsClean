package com.nm.fragmentsclean.ticketContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.ticketContext.read.projections.UserEntitlementsView;
import org.springframework.stereotype.Component;

@Component
public class GetUserEntitlementsQueryHandler
        implements QueryHandler<GetUserEntitlementsQuery, UserEntitlementsView> {

    private final UserEntitlementsReadRepository repository;

    public GetUserEntitlementsQueryHandler(UserEntitlementsReadRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserEntitlementsView handle(GetUserEntitlementsQuery query) {
        return repository.findByUserId(query.userId());
    }
}
