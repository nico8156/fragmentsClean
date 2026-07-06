package com.nm.fragmentsclean.ticketContext.read;

import com.nm.fragmentsclean.ticketContext.read.projections.UserEntitlementsView;
import java.util.UUID;

public interface UserEntitlementsReadRepository {
    UserEntitlementsView findByUserId(UUID userId);
}
