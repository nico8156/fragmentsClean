package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository {

    Optional<AppUser> findByAuthUserId(UUID authUserId);

    AppUser save(AppUser user);
}
