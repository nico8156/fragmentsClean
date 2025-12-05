package com.nm.fragmentsclean.authContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.List;

public record RefreshSessionCommand(
        String provider,
        String accessToken,
        String idToken,
        String refreshToken,
        long expiresAt,
        Long issuedAt,
        List<String> scopes,
        String existingUserId,
        Instant clientEstablishedAt
) implements Command {
}
