package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

import java.util.UUID;

public record GetCommandStatusQuery(UUID commandId, UUID requesterId) implements Query<CommandStatusView> {
}
