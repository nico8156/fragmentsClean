package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public final class GetAdministrativeCommandStatusQueryHandler
        implements QueryHandler<GetAdministrativeCommandStatusQuery, CommandStatusView> {
    private final CommandStatusReader statuses;

    public GetAdministrativeCommandStatusQueryHandler(CommandStatusReader statuses) {
        this.statuses = statuses;
    }

    @Override
    public CommandStatusView handle(GetAdministrativeCommandStatusQuery query) {
        return statuses.find(query.commandId());
    }
}
