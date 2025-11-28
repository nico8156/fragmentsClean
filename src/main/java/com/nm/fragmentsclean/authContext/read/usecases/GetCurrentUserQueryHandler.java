package com.nm.fragmentsclean.authContext.read.usecases;

import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.Identity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CurrentUserProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;

import java.util.List;
import java.util.UUID;

public class GetCurrentUserQueryHandler {

    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final IdentityRepository identityRepository;
    private final DateTimeProvider dateTimeProvider;

    public GetCurrentUserQueryHandler(
            CurrentUserProvider currentUserProvider,
            UserRepository userRepository,
            IdentityRepository identityRepository,
            DateTimeProvider dateTimeProvider
    ) {
        this.currentUserProvider = currentUserProvider;
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.dateTimeProvider = dateTimeProvider;
    }

    public GetCurrentUserResult execute() {
        UUID userId = currentUserProvider.currentUserId();

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "No user found for current user id: " + userId
                ));

        List<Identity> identities = identityRepository.listByUserId(userId);

        AppUserSnapshot snapshot = user.toSnapshot(identities);
        var now = dateTimeProvider.now();

        return new GetCurrentUserResult(snapshot, now);
    }
}
