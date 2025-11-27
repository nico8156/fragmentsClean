package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.security;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CurrentUserProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile({"fake", "database", "default"})
public class FakeCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UUID currentUserId() {
        // À ADAPTER selon la vraie signature de l'interface
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }
}
