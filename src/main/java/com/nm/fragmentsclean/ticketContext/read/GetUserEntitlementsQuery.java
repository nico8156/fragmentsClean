package com.nm.fragmentsclean.ticketContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.ticketContext.read.projections.UserEntitlementsView;
import java.util.UUID;

public record GetUserEntitlementsQuery(UUID userId) implements Query<UserEntitlementsView> {
}
