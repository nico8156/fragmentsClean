package com.nm.fragmentsclean.userApplicationContext.pass.application;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassSnapshot;
import java.util.UUID;

public record GetPassQuery(UUID userId) implements Query<PassSnapshot> { }
