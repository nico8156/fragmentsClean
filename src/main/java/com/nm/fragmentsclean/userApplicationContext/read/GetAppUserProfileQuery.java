package com.nm.fragmentsclean.userApplicationContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.userApplicationContext.read.projections.AppUserProfileView;
import java.util.UUID;

public record GetAppUserProfileQuery(UUID userId) implements Query<AppUserProfileView> {}
