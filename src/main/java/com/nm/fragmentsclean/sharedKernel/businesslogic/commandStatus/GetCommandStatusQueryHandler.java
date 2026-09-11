package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public final class GetCommandStatusQueryHandler implements QueryHandler<GetCommandStatusQuery, CommandStatusView> {
    private final CommandStatusReader statuses;

    public GetCommandStatusQueryHandler(CommandStatusReader statuses) {
        this.statuses = statuses;
    }

    @Override
    public CommandStatusView handle(GetCommandStatusQuery query) {
        return statuses.findForRequester(query.commandId(), query.requesterId());
    }
}
