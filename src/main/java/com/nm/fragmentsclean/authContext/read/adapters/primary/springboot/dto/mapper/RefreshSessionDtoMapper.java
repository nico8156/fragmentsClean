package com.nm.fragmentsclean.authContext.read.adapters.primary.springboot.dto.mapper;

import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.AppUserDto;
import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.LinkedIdentityDto;
import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.UserPreferencesDto;
import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.BadgeProgressDto;

import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.LinkedIdentitySnapshot;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.UserPreferencesSnapshot;
import com.nm.fragmentsclean.userContext.businesslogic.readmodels.BadgeProgressSnapshot;

import java.util.List;

public final class RefreshSessionDtoMapper {

    private RefreshSessionDtoMapper() {
    }

    // … ta méthode toResponse(RefreshSessionResult) etc.

    // 🔹 Doit être PUBLIC pour que le read side puisse l'appeler
    public static AppUserDto toAppUserDto(AppUserSnapshot snapshot) {
        return new AppUserDto(
                snapshot.id().toString(),                          // UUID -> String
                snapshot.createdAt().toString(),                   // Instant -> String
                snapshot.updatedAt().toString(),                   // Instant -> String
                snapshot.displayName(),
                snapshot.avatarUrl(),
                snapshot.bio(),
                toLinkedIdentityDtos(snapshot.identities()),
                snapshot.roles(),                                  // List<String>
                snapshot.flags(),                                  // Map<String, Boolean>
                toUserPreferencesDto(snapshot.preferences()),
                snapshot.likedCoffeeIds()
                        .stream()
                        .map(Object::toString)                     // UUID -> String
                        .toList(),
                snapshot.version()
        );
    }

    private static List<LinkedIdentityDto> toLinkedIdentityDtos(List<LinkedIdentitySnapshot> snapshots) {
        return snapshots.stream()
                .map(RefreshSessionDtoMapper::toLinkedIdentityDto)
                .toList();
    }

    private static LinkedIdentityDto toLinkedIdentityDto(LinkedIdentitySnapshot snap) {
        return new LinkedIdentityDto(
                snap.id().toString(),
                snap.provider(),
                snap.providerUserId(),
                snap.email(),
                snap.createdAt().toString(),
                snap.lastAuthAt() != null ? snap.lastAuthAt().toString() : null
        );
    }

    private static UserPreferencesDto toUserPreferencesDto(UserPreferencesSnapshot prefs) {
        return new UserPreferencesDto(
                prefs.locale(),
                prefs.marketingOptIn(),
                prefs.pushOptIn(),
                prefs.theme(),
                toBadgeProgressDto(prefs.badgeProgress())
        );
    }

    private static BadgeProgressDto toBadgeProgressDto(BadgeProgressSnapshot bp) {
        return new BadgeProgressDto(
                bp.exploration(),
                bp.gout(),
                bp.social(),
                bp.unlockedBadges()
        );
    }

    // … ton toAuthTokensDto(AppSessionTokens tokens) etc.
}
