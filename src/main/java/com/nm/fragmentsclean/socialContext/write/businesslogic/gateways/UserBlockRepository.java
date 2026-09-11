package com.nm.fragmentsclean.socialContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.UserBlock;
import java.util.Optional;
import java.util.UUID;

public interface UserBlockRepository {
    Optional<UserBlock> byUsers(UUID blockerId, UUID blockedUserId);
    void save(UserBlock block);
}
