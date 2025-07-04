package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Command;

import java.util.UUID;

public record MakeLikeCommand(
        UUID likeId,
        UUID userId,
        UUID targetId
) implements Command { }
