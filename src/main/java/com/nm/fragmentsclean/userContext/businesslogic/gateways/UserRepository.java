package com.nm.fragmentsclean.userContext.businesslogic.gateways;

import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<AppUser> findById(UUID userId);
    AppUser save(AppUser user);
}
