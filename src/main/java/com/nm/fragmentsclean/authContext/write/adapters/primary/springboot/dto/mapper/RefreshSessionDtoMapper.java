package com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.mapper;

import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.*;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.AppSessionTokens;
import com.nm.fragmentsclean.authContext.write.businesslogic.usecases.RefreshSessionCommand;
import com.nm.fragmentsclean.authContext.write.businesslogic.usecases.RefreshSessionResult;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.BadgeProgressSnapshot;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.LinkedIdentitySnapshot;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.UserPreferencesSnapshot;

public final class RefreshSessionDtoMapper {

    private RefreshSessionDtoMapper() {
    }

    // ---------- DTO -> Command ----------

    public static RefreshSessionCommand toCommand(RefreshSessionRequestDto dto) {
        long expiresAt = dto.sessionExpiresAt() != null
                ? dto.sessionExpiresAt().getEpochSecond()
                : 0L;

        Long issuedAt = dto.sessionEstablishedAt() != null
                ? dto.sessionEstablishedAt().getEpochSecond()
                : null;

        return new RefreshSessionCommand(
                dto.provider(),
                dto.accessToken(),
                dto.idToken(),
                null,                      // refreshToken (pas encore géré côté front)
                expiresAt,
                issuedAt,
                dto.scopes(),
                null,                      // existingUserId (pour plus tard)
                dto.sessionEstablishedAt() // clientEstablishedAt
        );
    }

    // ---------- Result -> DTO ----------

    public static RefreshSessionResponseDto toResponse(RefreshSessionResult result) {
        AppUserDto userDto = toAppUserDto(result.user());
        AuthTokensDto tokensDto = toAuthTokensDto(result.tokens());

        return new RefreshSessionResponseDto(
                userDto,
                tokensDto,
                result.provider(),
                result.scopes()
        );
    }

    // ---------- Helpers : snapshots -> DTO ----------

    public static AppUserDto toAppUserDto(AppUserSnapshot snapshot) {
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

    private static LinkedIdentityDto toLinkedIdentityDto(LinkedIdentitySnapshot snapshot) {
        return new LinkedIdentityDto(
                snapshot.id(),
                snapshot.provider(),
                snapshot.providerUserId(),
                snapshot.email(),
                snapshot.createdAt(),
                snapshot.lastAuthAt()
        );
    }

    private static UserPreferencesDto toUserPreferencesDto(UserPreferencesSnapshot snapshot) {
        return new UserPreferencesDto(
                snapshot.locale(),
                snapshot.marketingOptIn(),
                snapshot.pushOptIn(),
                snapshot.theme(),
                toBadgeProgressDto(snapshot.badgeProgress())
        );
    }

    private static BadgeProgressDto toBadgeProgressDto(BadgeProgressSnapshot snapshot) {
        return new BadgeProgressDto(
                snapshot.exploration(),
                snapshot.gout(),
                snapshot.social(),
                snapshot.unlockedBadges()
        );
    }

    private static AuthTokensDto toAuthTokensDto(AppSessionTokens tokens) {
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
