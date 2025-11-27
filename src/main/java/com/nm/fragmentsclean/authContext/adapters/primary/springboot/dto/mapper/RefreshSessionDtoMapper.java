package com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto.mapper;

import com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto.*;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionCommand;
import com.nm.fragmentsclean.authContext.businesslogic.usecases.RefreshSessionResult;
// à adapter selon l’emplacement réel :
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;

public final class RefreshSessionDtoMapper {

    private RefreshSessionDtoMapper() {
    }

    // ---------- DTO -> Command ----------

    public static RefreshSessionCommand toCommand(RefreshSessionRequestDto dto) {
        // On dérive les epoch seconds à partir des Instant, si présents
        long expiresAt = dto.sessionExpiresAt() != null
                ? dto.sessionExpiresAt().getEpochSecond()
                : 0L;

        Long issuedAt = dto.sessionEstablishedAt() != null
                ? dto.sessionEstablishedAt().getEpochSecond()
                : null;

        return new RefreshSessionCommand(
                dto.provider(),            // provider
                dto.accessToken(),         // accessToken (tel que reçu du provider)
                dto.idToken(),             // idToken
                null,                      // refreshToken (pas encore géré côté front)
                expiresAt,                 // expiresAt (provider)
                issuedAt,                  // issuedAt (provider)
                dto.scopes(),              // scopes
                null,                      // existingUserId (pour plus tard, quand tu voudras le passer explicitement)
                dto.sessionEstablishedAt() // clientEstablishedAt : quand le client a établi la session
        );
    }


    // ---------- Result -> DTO ----------

    public static RefreshSessionResponseDto toResponse(RefreshSessionResult result) {
        // Là aussi, adapte les getters aux vrais noms de ton Result
        AppUserDto userDto = toAppUserDto(result.user());
        AuthTokensDto tokensDto = toAuthTokensDto(result.tokens());

        return new RefreshSessionResponseDto(
                userDto,
                tokensDto,
                result.provider(),
                result.scopes()
        );
    }

    // ---------- Helpers pour snapshots -> DTO existants ----------

    private static AppUserDto toAppUserDto(AppUserSnapshot snapshot) {
        return new AppUserDto(
                snapshot.id(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                snapshot.displayName(),
                snapshot.avatarUrl(),
                snapshot.bio(),
                snapshot.identities().stream()
                        .map(RefreshSessionDtoMapper::toLinkedIdentityDto)
                        .toList(),
                snapshot.roles(),
                snapshot.flags(),
                toUserPreferencesDto(snapshot.preferences()),
                snapshot.likedCoffeeIds(),
                snapshot.version()
        );
    }


    private static LinkedIdentityDto toLinkedIdentityDto(
            com.nm.fragmentsclean.userContext.businesslogic.readmodels.LinkedIdentitySnapshot snapshot
    ) {
        return new LinkedIdentityDto(
                snapshot.id(),
                snapshot.provider(),
                snapshot.providerUserId(),
                snapshot.email(),
                snapshot.createdAt(),
                snapshot.lastAuthAt()
        );
    }

    private static BadgeProgressDto toBadgeProgressDto(
            com.nm.fragmentsclean.userContext.businesslogic.readmodels.BadgeProgressSnapshot snapshot
    ) {
        return new BadgeProgressDto(
                snapshot.exploration(),
                snapshot.gout(),
                snapshot.social(),
                snapshot.unlockedBadges()
        );
    }

    private static UserPreferencesDto toUserPreferencesDto(
            com.nm.fragmentsclean.userContext.businesslogic.readmodels.UserPreferencesSnapshot snapshot
    ) {
        return new UserPreferencesDto(
                snapshot.locale(),
                snapshot.marketingOptIn(),
                snapshot.pushOptIn(),
                snapshot.theme(),
                toBadgeProgressDto(snapshot.badgeProgress())
        );
    }


    private static AuthTokensDto toAuthTokensDto(
            com.nm.fragmentsclean.authContext.businesslogic.models.AppSessionTokens tokens
    ) {
        return new AuthTokensDto(
                tokens.accessToken(),
                tokens.idToken(),
                tokens.refreshToken(),
                tokens.expiresAt(),
                tokens.issuedAt(),
                tokens.tokenType(),
                tokens.scope()
        );
    }
}
