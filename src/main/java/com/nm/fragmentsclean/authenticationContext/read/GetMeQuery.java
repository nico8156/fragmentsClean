package com.nm.fragmentsclean.authenticationContext.read;

import com.nm.fragmentsclean.authenticationContext.read.projections.AuthMeView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

import java.util.UUID;

public record GetMeQuery(UUID userId) implements Query<AuthMeView> {}
